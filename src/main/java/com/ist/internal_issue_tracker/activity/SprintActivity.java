package com.ist.internal_issue_tracker.activity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One thing that happened to one sprint. Append-only, and shaped exactly like {@link IssueActivity}
 * - see it for why the ids are plain integers and why {@code createdAt} is not a
 * {@code @CreationTimestamp}.
 *
 * <p>No {@code projectId} here, because the table has none. A sprint's history is read through the
 * sprint, and the project it belongs to is resolved through {@code SprintLookup}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "sprint_activities")
public class SprintActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer sprintId;

  /** Who made the change. */
  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer userId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private SprintActionType actionType;

  @Size(max = 255)
  @Column(updatable = false)
  private String oldValue;

  @Size(max = 255)
  @Column(updatable = false)
  private String newValue;

  /** Taken verbatim from the event - see {@code IssueActivity#createdAt}. */
  @Column(nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
