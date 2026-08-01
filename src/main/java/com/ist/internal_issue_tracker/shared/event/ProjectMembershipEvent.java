package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;

/**
 * Someone or some team joining or leaving a project.
 *
 * <p>One record rather than four, because the four cases differ only in two independent flags and
 * the alternative would be four files that are otherwise identical. {@code subjectId} is the user or
 * the team being added or removed - never the actor, who is whoever is doing the adding.
 *
 * <p>These are not {@link ProjectChangedEvent} changes: membership has no old value to move from, so
 * a field-change shape would have to leave one side permanently null and say nothing the action type
 * does not already say.
 *
 * <p>See {@link IssueCreatedEvent} for why delivery is asynchronous and why the timestamp travels
 * with the event.
 */
public record ProjectMembershipEvent(
    Integer projectId,
    Integer subjectId,
    Subject subject,
    Change change,
    Integer actorId,
    OffsetDateTime occurredAt) {

  /** Whether {@code subjectId} names a user or a team. */
  public enum Subject {
    USER,
    TEAM
  }

  public enum Change {
    ADDED,
    REMOVED
  }
}
