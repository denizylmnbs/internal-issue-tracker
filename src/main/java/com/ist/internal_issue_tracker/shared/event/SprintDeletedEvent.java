package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import org.springframework.modulith.events.Externalized;

/**
 * Published when a sprint is soft-deleted. The status is not carried: a deleted sprint keeps
 * whatever status it held, which the last {@code STATUS_UPDATED} row already records - see {@code
 * SprintService#deleteSprint} for why that status is deliberately left alone. {@code projectId}
 * travels with it for the reason given on {@link SprintCreatedEvent}.
 */
@Externalized("sprint-events::#{#this.sprintId().toString()}")
public record SprintDeletedEvent(
    Integer sprintId, Integer projectId, Integer actorId, OffsetDateTime occurredAt) {}
