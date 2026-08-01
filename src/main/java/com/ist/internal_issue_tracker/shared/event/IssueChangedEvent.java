package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Published when a live issue is edited, in any of the ways that leave a trace worth keeping.
 *
 * <p>It carries a <em>list</em> of changes rather than being one event per field because a single
 * call can move several at once - {@code updateIssue} may change the name, the priority, the story
 * point and the sprint together - and those land in the audit log as separate rows with separate
 * action types. Splitting them into separate events would mean publishing four events for one user
 * action, and would lose the fact that they happened together.
 *
 * <p>All rows produced from one event share its {@code occurredAt}, because they describe one moment
 * and not four. Anything reading this history in order therefore has to break ties on something
 * else - the activity row's own id - or a window function will order them arbitrarily and can
 * produce spans of zero or negative length.
 *
 * <p>An event with an empty {@code changes} list is never published: an update that sets every field
 * to the value it already had is not a change, and recording it would put noise into a log whose
 * whole value is that every row means something happened. Callers check before publishing.
 *
 * <p>See {@link IssueCreatedEvent} for why delivery is asynchronous and why the timestamp travels
 * with the event.
 */
public record IssueChangedEvent(
    Integer issueId,
    Integer projectId,
    Integer actorId,
    OffsetDateTime occurredAt,
    List<IssueFieldChange> changes) {}
