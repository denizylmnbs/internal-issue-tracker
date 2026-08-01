package com.ist.internal_issue_tracker.shared.event;

/**
 * One field moving from one value to another, as carried by {@link IssueChangedEvent}.
 *
 * <p>Both values are strings because the audit columns they end up in are, and because a change set
 * holds fields of different types at once - a status, a story point and a name cannot share a typed
 * slot without a wrapper that would buy nothing. The rule for producing them: <em>the readable
 * rendering of the value, short enough for a 255-character column, or null where the value has no
 * meaningful rendering.</em> A null is not "unknown"; it means the field was empty on that side of
 * the change, which is how an issue gaining or losing an assignee is expressed.
 *
 * <p>Ids are rendered as their decimal string rather than resolved to names. Resolving would mean
 * this module reading {@code users} or {@code sprints}, and a name copied into an audit row goes
 * stale the moment it is edited - the id stays true, and the reader can resolve it if it wants.
 */
public record IssueFieldChange(IssueField field, String oldValue, String newValue) {}
