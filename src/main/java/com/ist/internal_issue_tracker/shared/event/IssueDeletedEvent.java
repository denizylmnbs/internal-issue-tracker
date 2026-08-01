package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;

/**
 * Published when an issue is soft-deleted. The status is deliberately not carried: the issue keeps
 * whatever status it had when it was dropped, so the last {@code STATUS_UPDATED} row already says
 * where it stood and repeating it here would invite the two to disagree.
 *
 * <p>This is the event the deletion history starts from. Issues deleted before the activity log
 * existed have no row and cannot get one - {@code issues.deleted_at} records when, but nothing
 * records who, and an audit row naming the wrong person is worse than an absent one.
 *
 * <p>See {@link IssueCreatedEvent} for why delivery is asynchronous and why the timestamp travels
 * with the event.
 */
public record IssueDeletedEvent(
    Integer issueId, Integer projectId, Integer actorId, OffsetDateTime occurredAt) {}
