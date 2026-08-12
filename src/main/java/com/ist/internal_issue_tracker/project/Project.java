package com.ist.internal_issue_tracker.project;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "projects")
public class Project {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotBlank
  @Size(min = 2, max = 255)
  @Column(nullable = false, unique = true)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  /**
   * {@code LocalDate}, not {@code OffsetDateTime}: the column is a bare {@code date}, and a project
   * starting "on the 3rd" means the same day in every timezone.
   */
  @NotNull
  @Column(nullable = false)
  private LocalDate startDate;

  /** Optional - an open-ended project has no planned finish. */
  private LocalDate endDate;

  /**
   * Nullable: a project can exist before anyone is put in charge of it, and a leader is chosen
   * later through its own endpoint. While it is null nobody passes the "leads this project" branch
   * of the authorization rule, so only editors can act on the project.
   *
   * <p>Plain user id rather than a {@code @ManyToOne User}: an association would pull a JPA
   * dependency across the module boundary into {@code user} and break {@code ModularityTests}. The
   * referenced user is validated in the service layer; the foreign key is enforced by the database.
   */
  private Integer leaderId;

  /**
   * A code from the global {@code PROJECT_STATUS} field definitions, not a fixed enum - see {@code
   * FieldDefinition}. Global rather than project-scoped, unlike the other six kinds: a project
   * cannot own the vocabulary used to list projects. {@code ProjectService} resolves the default on
   * create and validates every write through {@code FieldDefinitionLookup}.
   */
  @NotBlank
  @Column(nullable = false, length = 30)
  private String status;

  /** Soft-delete flag. Independent of {@link #status} - see {@link ProjectStatus}. */
  @NotNull
  @Column(nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp private OffsetDateTime updatedAt;
}
