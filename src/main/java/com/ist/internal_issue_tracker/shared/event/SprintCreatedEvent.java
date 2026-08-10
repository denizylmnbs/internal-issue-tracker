package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import org.springframework.modulith.events.Externalized;

/**
 * Published when a sprint is opened. See {@link IssueCreatedEvent} for why delivery goes over Kafka,
 * why {@code occurredAt} travels with the event rather than being read by the consumer, and what may
 * and may not change about this record now that it is on the wire.
 *
 * <p>Keyed on the sprint rather than the project: the ordering that has to hold is between the
 * events of one sprint, and keying on {@code projectId} would crowd every sprint in a busy project
 * onto one partition for no gain.
 *
 * <p>Carries {@code projectId} for the same reason the issue events do: {@code sprint_activities}
 * now has a column for it (V4__sprint_activities_project_id.sql), so the project-wide activity feed
 * can union this table with {@code project_activities} and {@code issue_activities} without the
 * {@code activity} module reading {@code sprint}'s own table to resolve it.
 */
@Externalized("sprint-events::#{#this.sprintId().toString()}")
public record SprintCreatedEvent(
    Integer sprintId, Integer projectId, Integer actorId, OffsetDateTime occurredAt) {}
