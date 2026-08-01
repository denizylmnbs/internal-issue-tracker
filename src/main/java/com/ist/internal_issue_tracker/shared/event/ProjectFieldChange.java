package com.ist.internal_issue_tracker.shared.event;

/**
 * One project field moving, under the value rules given on {@link IssueFieldChange}.
 *
 * <p>{@code LEADER} renders the user id, and a null on either side is meaningful rather than
 * missing: a project may be created without a leader and may have one taken away again.
 */
public record ProjectFieldChange(ProjectField field, String oldValue, String newValue) {}
