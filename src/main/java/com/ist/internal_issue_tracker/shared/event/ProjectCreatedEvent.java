package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import org.springframework.modulith.events.Externalized;

/**
 * Published when a project is opened. See {@link IssueCreatedEvent} for why delivery is
 * asynchronous and why {@code occurredAt} travels with the event.
 */
@Externalized("project-events::#{#this.projectId().toString()}")
public record ProjectCreatedEvent(Integer projectId, Integer actorId, OffsetDateTime occurredAt) {}
