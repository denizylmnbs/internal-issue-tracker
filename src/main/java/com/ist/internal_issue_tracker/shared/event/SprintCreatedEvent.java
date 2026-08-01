package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;

/**
 * Published when a sprint is opened. See {@link IssueCreatedEvent} for why delivery is asynchronous
 * and why {@code occurredAt} travels with the event rather than being read by the consumer.
 *
 * <p>No {@code projectId}, unlike the issue events. {@code sprint_activities} has no column for it,
 * and a field an event carries that nothing can store is a field that will drift out of meaning. A
 * sprint's activity is read through its sprint, which the {@code SprintLookup} port already ties
 * back to a project.
 */
public record SprintCreatedEvent(Integer sprintId, Integer actorId, OffsetDateTime occurredAt) {}
