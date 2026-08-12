package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import org.springframework.modulith.events.Externalized;

/**
 * Published when an issue is soft-deleted. The status is deliberately not carried: the issue keeps
 * whatever status it had when it was dropped, so the last {@code STATUS_UPDATED} row already says
 * where it stood and repeating it here would invite the two to disagree.
 *
 * <p>This is the event the deletion history starts from. Issues deleted before the activity log
 * existed have no row and cannot get one - {@code issues.deleted_at} records when, but nothing
 * records who, and an audit row naming the wrong person is worse than an absent one.
 *
 * <p>{@code dimensions} is carried even though nothing is being changed, because the deletion is
 * what removes the issue from a sprint's scope: a burndown reading the log forward has to know how
 * many points left the sprint on that day, and the DELETED row is the only place that can say. See
 * {@link IssueDimensions}.
 *
 * <p>See {@link IssueCreatedEvent} for why delivery is asynchronous and why the timestamp travels
 * with the event.
 */
@Externalized("issue-events::#{#this.issueId().toString()}")
public record IssueDeletedEvent(
    Integer issueId,
    Integer projectId,
    Integer actorId,
    OffsetDateTime occurredAt,
    IssueDimensions dimensions) {}
