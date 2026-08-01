package com.ist.internal_issue_tracker.activity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One thing that happened to one issue. Append-only: nothing here is ever updated or deleted, which
 * is why there is no {@code updatedAt} and no soft-delete stamp - an issue being dropped is itself
 * recorded as a row rather than by hiding the rows before it.
 *
 * <p>Every id is a plain {@code Integer} rather than an association, for the reason given on
 * {@code Issue}: four of the five targets live in other modules and a {@code @ManyToOne} would break
 * {@code ModularityTests}. Here the argument is stronger than usual - the history must survive the
 * thing it describes, and an association would invite a join that quietly drops rows whose subject
 * has been retired.
 *
 * <p>{@code projectId} is copied from the event rather than looked up, matching the column the
 * migration denormalised. It cannot go stale because an issue never moves between projects, and it
 * is what lets every metric filter by project without this module reading {@code issues}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "issue_activities")
public class IssueActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer issueId;

  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer projectId;

  /** Who made the change - not the issue's reporter, and not its assignee. */
  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer userId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private IssueActionType actionType;

  /**
   * Both sides of the change, as rendered by the publisher. Null on either side means the field was
   * empty there - an issue gaining its first assignee has a null {@code oldValue} - and both are
   * null on {@code CREATED} and {@code DELETED}, which are not field changes at all.
   */
  @Size(max = 255)
  @Column(updatable = false)
  private String oldValue;

  @Size(max = 255)
  @Column(updatable = false)
  private String newValue;

  /**
   * When the change happened, taken verbatim from the event.
   *
   * <p>Deliberately <b>not</b> {@code @CreationTimestamp}, unlike every other entity here. The
   * listener that writes this row is asynchronous and runs after the publisher's transaction has
   * committed, so Hibernate's persist moment is not the moment of the change; after a restart
   * replays outstanding publications it may not even be the same day. Every metric on this table is
   * a difference between two of these timestamps, so whatever gap crept in here would be read back
   * as cycle time. The publisher takes the reading, once, alongside the change itself.
   */
  @Column(nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
