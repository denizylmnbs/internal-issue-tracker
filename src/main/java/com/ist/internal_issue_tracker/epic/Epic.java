package com.ist.internal_issue_tracker.epic;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A large body of work on one project, which issues are filed under.
 *
 * <p>Soft delete is a {@code deletedAt} stamp, as in {@code Sprint} and for the same reason: the
 * schema was drawn that way and it records when the epic was dropped rather than merely that it
 * was. A live row is one whose {@code deletedAt} is null.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "epics")
public class Epic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  /**
   * Plain project id rather than a {@code @ManyToOne Project} - an association would pull a JPA
   * dependency across the module boundary and break {@code ModularityTests}. The project is
   * validated through {@code ProjectLookup}; the foreign key is the database's job.
   */
  @NotNull
  @Column(nullable = false)
  private Integer projectId;

  /**
   * Unique per project among live epics only - {@code unique_active_epic_name_per_project} is a
   * partial index, so deleting an epic hands its name back for reuse.
   */
  @NotBlank
  @Size(min = 2, max = 255)
  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  /**
   * A code from this project's {@code EPIC_STATUS} field definitions, not a fixed enum - see
   * {@code FieldDefinition}. {@code EpicService} resolves the default on create and validates every
   * write through {@code FieldDefinitionLookup}; nothing here can enforce that on its own anymore.
   */
  @NotBlank
  @Column(nullable = false, length = 30)
  private String status;

  /**
   * Who opened the epic, taken from the authenticated caller and never from the request body.
   *
   * <p>{@code updatable = false} because this is a historical fact rather than a setting: no
   * endpoint changes it, and marking the column that way means no future one can do so by accident.
   * A plain user id for the same boundary reason as {@link #projectId}.
   */
  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer reporterId;

  /** Null while the epic is live; the moment it was dropped once it is not. */
  private OffsetDateTime deletedAt;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp private OffsetDateTime updatedAt;
}
