package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Published when a live sprint is edited. Carries a list for the reason {@link IssueChangedEvent}
 * does - one call may move the name and both dates - and, like it, is never published with an empty
 * one.
 *
 * <p>All rows from one event share its {@code occurredAt}.
 */
public record SprintChangedEvent(
    Integer sprintId,
    Integer actorId,
    OffsetDateTime occurredAt,
    List<SprintFieldChange> changes) {}
