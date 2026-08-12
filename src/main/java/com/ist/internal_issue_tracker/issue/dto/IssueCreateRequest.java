package com.ist.internal_issue_tracker.issue.dto;

import com.ist.internal_issue_tracker.issue.IssuePriority;
import com.ist.internal_issue_tracker.issue.IssueType;
import com.ist.internal_issue_tracker.issue.IssueUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Neither the project nor the reporter is a field here - the project comes from the path and the
 * reporter from the authenticated caller.
 *
 * <p>{@code status} is absent, as on every other create request in this codebase: an issue is born
 * in {@code BACKLOG} and moves on through its own endpoint. {@code priority} <em>is</em> accepted,
 * because it is not a lifecycle position - filing something as {@code CRITICAL} from the start is
 * ordinary. Leaving it out takes the entity's {@code MEDIUM} default.
 *
 * <p>Assignees may be set here even though they also have their own endpoint. Filing work already
 * pointed at someone is common enough to be worth the second path, and both run the same
 * validation.
 */
public record IssueCreateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description,
    @NotNull(message = "Type cannot be null") IssueType type,
    IssuePriority priority,
    IssueUnit resolvingUnit,
    @PositiveOrZero(message = "Story point cannot be negative") Integer storyPoint,
    Integer sprintId,
    Integer epicId,
    Integer assigneeUserId,
    Integer assigneeTeamId) {}
