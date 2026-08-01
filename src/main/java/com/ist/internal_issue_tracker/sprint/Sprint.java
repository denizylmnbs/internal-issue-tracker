package com.ist.internal_issue_tracker.sprint;

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

/**
 * A time-boxed slice of work on one project.
 *
 * <p>Soft delete here is a {@code deletedAt} stamp rather than the {@code isActive} flag the older
 * modules use. The schema was drawn that way and it carries more: it says when the sprint was
 * dropped, not merely that it was. A live row is one whose {@code deletedAt} is null, and every
 * query in {@link SprintRepository} says so explicitly.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "sprints")
public class Sprint {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  /**
   * Plain project id rather than a {@code @ManyToOne Project}: an association would pull a JPA
   * dependency across the module boundary into {@code project} and break {@code ModularityTests}.
   * The project is validated through {@code ProjectLookup}; the foreign key is the database's job.
   */
  @NotNull
  @Column(nullable = false)
  private Integer projectId;

  /**
   * Unique per project among live sprints only - {@code unique_active_sprint_name_per_project} is a
   * partial index, so deleting a sprint hands its name back for reuse.
   */
  @NotBlank
  @Size(min = 2, max = 255)
  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  /** {@code LocalDate}, not {@code OffsetDateTime} - see {@code Project#startDate}. */
  @NotNull
  @Column(nullable = false)
  private LocalDate startDate;

  /** Optional - a sprint may be opened before anyone commits to when it ends. */
  private LocalDate endDate;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SprintStatus status = SprintStatus.TODO;

  /**
   * The story points in the sprint at the moment it started, and the moment it started.
   *
   * <p>Written once, by {@code SprintService#changeStatus} on the first move to {@code IN_PROGRESS},
   * and never again - not by {@code updateSprint}, not by a later status change, not by a request
   * body, which is why neither appears on {@code SprintUpdateRequest}. That is the point of them: a
   * commitment that can be revised after the fact cannot be missed, and a velocity built on one says
   * nothing. Work pulled in mid-sprint has to show up as scope the team did not promise.
   *
   * <p>Delivered points are <em>not</em> stored here. Those come from the activity log, where the
   * DONE row carries the estimate the issue had when it got there - see {@code IssueDimensions}. The
   * two numbers are kept in different places on purpose: one is a decision, the other is an
   * observation, and only the decision has to be written down at the time.
   *
   * <p>Both are null on a sprint that has never started, and both are null on sprints started before
   * {@code V3}. {@code committedAt} is what tells that apart from a sprint started with nothing in
   * it, which is a real state and reads zero.
   */
  private Integer committedPoints;

  private OffsetDateTime committedAt;

  /** Null while the sprint is live; the moment it was dropped once it is not. */
  private OffsetDateTime deletedAt;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp private OffsetDateTime updatedAt;
}
