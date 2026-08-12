package com.ist.internal_issue_tracker.issue;

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
 * A single piece of work on a project - the most connected row in the schema, pointing at a
 * project, optionally a sprint and an epic, a reporter, and up to two assignees.
 *
 * <p>Every one of those is a plain {@code Integer} rather than an association. Four of the six
 * targets live in other modules, so a {@code @ManyToOne} would break {@code ModularityTests}; the
 * remaining ones are kept plain for consistency. References are validated through ports in the
 * service, and the foreign keys are the database's job.
 *
 * <p>Soft delete is a {@code deletedAt} stamp, as in {@code Sprint} and {@code Epic}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "issues")
public class Issue {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(nullable = false)
  private Integer projectId;

  /** Optional, and must belong to {@link #projectId} when set - unplanned work has no sprint. */
  private Integer sprintId;

  /** Optional, and must belong to {@link #projectId} when set. */
  private Integer epicId;

  /**
   * Nullable in the schema, required by the API. Widening it later is a migration-free change;
   * narrowing it would not have been, which is why the API is the stricter of the two.
   */
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private IssueType type;

  /** Which unit resolves the issue - optional, set once triage assigns it to a team. */
  @Enumerated(EnumType.STRING)
  @Column(name = "resolving_unit", length = 20)
  private IssueUnit resolvingUnit;

  /** No uniqueness of any kind - two issues on one project may share a name. */
  @NotBlank
  @Size(min = 2, max = 255)
  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IssueStatus status = IssueStatus.BACKLOG;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IssuePriority priority = IssuePriority.MEDIUM;

  /** Estimate, in whatever unit the team has agreed on. Null until someone sizes the work. */
  private Integer storyPoint;

  /** Who filed it, taken from the authenticated caller - see {@code Epic#reporterId}. */
  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer reporterId;

  /**
   * The two assignees are independent, not alternatives: an issue may be handed to a team and then
   * picked up by one of its members, in which case both are set. Either may be null on its own.
   */
  private Integer assigneeTeamId;

  private Integer assigneeUserId;

  /** Null while the issue is live; the moment it was dropped once it is not. */
  private OffsetDateTime deletedAt;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp private OffsetDateTime updatedAt;
}
