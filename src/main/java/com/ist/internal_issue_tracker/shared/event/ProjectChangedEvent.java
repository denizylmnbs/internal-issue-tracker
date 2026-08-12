package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.modulith.events.Externalized;

/**
 * Published when a live project is edited. Carries a list for the reason {@link IssueChangedEvent}
 * does, is never published with an empty one, and stamps every row it produces with one {@code
 * occurredAt}.
 */
@Externalized("project-events::#{#this.projectId().toString()}")
public record ProjectChangedEvent(
    Integer projectId,
    Integer actorId,
    OffsetDateTime occurredAt,
    List<ProjectFieldChange> changes) {}
