package com.ist.internal_issue_tracker.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Neither the project nor the reporter is a field here - the project comes from the path and the
 * reporter from the authenticated caller.
 *
 * <p>{@code status} is absent, as on every other create request in this codebase: an issue is born
 * at its project's {@code ISSUE_STATUS} default and moves on through its own endpoint. {@code
 * priority} <em>is</em> accepted, because it is not a lifecycle position - filing something as
 * {@code CRITICAL} from the start is ordinary. Leaving it out takes the project's {@code
 * ISSUE_PRIORITY} default.
 *
 * <p>{@code type}, {@code priority} and {@code resolvingUnit} are codes from this project's
 * respective field definitions, validated by {@code IssueService} - not fixed enums.
 *
 * <p>Assignees may be set here even though they also have their own endpoint. Filing work already
 * pointed at someone is common enough to be worth the second path, and both run the same
 * validation.
 */
public record IssueCreateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description,
    @NotBlank(message = "Type cannot be blank") String type,
    String priority,
    String resolvingUnit,
    @PositiveOrZero(message = "Story point cannot be negative") Integer storyPoint,
    Integer sprintId,
    Integer epicId,
    Integer assigneeUserId,
    Integer assigneeTeamId) {}
