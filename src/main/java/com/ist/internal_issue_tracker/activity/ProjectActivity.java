package com.ist.internal_issue_tracker.activity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One thing that happened to one project. Append-only and shaped like {@link IssueActivity} - see it
 * for why the ids are plain integers and why {@code createdAt} is not a {@code @CreationTimestamp}.
 *
 * <p>On a membership row, {@code newValue} carries the id of the person or team added and {@code
 * oldValue} the id of the one removed, so the value columns keep meaning "what it was" and "what it
 * is" rather than being repurposed into a subject column.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "project_activities")
public class ProjectActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer projectId;

  /** Who made the change - not the person being added or removed by it. */
  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer userId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private ProjectActionType actionType;

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
