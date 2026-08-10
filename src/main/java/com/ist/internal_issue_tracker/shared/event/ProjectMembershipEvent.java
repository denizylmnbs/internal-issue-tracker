package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import org.springframework.modulith.events.Externalized;

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
 * <p><b>Delivered both ways</b>, and the only event here that is. Externalising a record does not
 * stop it reaching listeners in this process, so the audit half travels to {@code project-events}
 * and is written by {@code ProjectActivityListener}, while {@code ProjectParticipantCacheEviction
 * Listener} keeps consuming the very same publication inline. The two halves want opposite things -
 * one must not be able to fail the request, the other must finish before anyone can read a stale
 * participant - and neither has to give way.
 *
 * <p>See {@link IssueCreatedEvent} for why the timestamp travels with the event and what may and may
 * not change about this record now that it is on the wire.
 */
@Externalized("project-events::#{#this.projectId().toString()}")
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
