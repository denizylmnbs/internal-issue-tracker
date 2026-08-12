package com.ist.internal_issue_tracker.issue.dto;

import com.ist.internal_issue_tracker.issue.IssuePriority;
import com.ist.internal_issue_tracker.issue.IssueType;
import com.ist.internal_issue_tracker.issue.IssueUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * A full replacement of the details. Being a replacement rather than a patch is what makes the
 * nullable fields unambiguous: omitting {@code sprintId} means the issue leaves its sprint, because
 * the whole record is being restated rather than nudged.
 *
 * <p>Status and assignees are not here - each has its own endpoint, so the two things a board
 * changes one at a time cannot be clobbered by someone saving an edit form at the same moment. The
 * reporter is not editable at all.
 */
public record IssueUpdateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description,
    @NotNull(message = "Type cannot be null") IssueType type,
    @NotNull(message = "Priority cannot be null") IssuePriority priority,
    IssueUnit resolvingUnit,
    @PositiveOrZero(message = "Story point cannot be negative") Integer storyPoint,
    Integer sprintId,
    Integer epicId) {}
