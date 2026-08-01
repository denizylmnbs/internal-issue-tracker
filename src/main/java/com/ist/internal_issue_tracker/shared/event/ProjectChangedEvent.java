package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Published when a live project is edited. Carries a list for the reason {@link IssueChangedEvent}
 * does, is never published with an empty one, and stamps every row it produces with one
 * {@code occurredAt}.
 */
public record ProjectChangedEvent(
    Integer projectId,
    Integer actorId,
    OffsetDateTime occurredAt,
    List<ProjectFieldChange> changes) {}
