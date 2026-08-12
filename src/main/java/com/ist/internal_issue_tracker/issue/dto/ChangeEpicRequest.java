package com.ist.internal_issue_tracker.issue.dto;

/**
 * The epic counterpart of {@link ChangeSprintRequest} - null means the issue belongs to no epic,
 * for the same reason given there.
 *
 * <p>Unlike every other narrow change, this one leaves no activity row: {@code issue_activities}
 * has no action type for the epic, so {@code IssueSnapshot} does not carry it and the detector has
 * nothing to compare. See the note on {@code IssueSnapshot}.
 */
public record ChangeEpicRequest(Integer epicId) {}
