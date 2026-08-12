package com.ist.internal_issue_tracker.issue.dto;

/**
 * Moves the issue into a sprint, or out of every sprint when {@code sprintId} is null. Nothing is
 * validated because null is not the absence of an answer here - it is the answer "the backlog", and
 * the field being the whole body is what makes the two impossible to confuse.
 *
 * <p>This is the narrow counterpart of {@link IssueUpdateRequest}, which can also move an issue
 * between sprints but only by restating every other field along with it. Planning a sprint means
 * doing this to a dozen issues in a row, and doing it through the full replacement means carrying
 * each issue's description and estimate through the round trip to leave them where they were.
 */
public record ChangeSprintRequest(Integer sprintId) {}
