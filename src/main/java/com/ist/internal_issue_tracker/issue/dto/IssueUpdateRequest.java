package com.ist.internal_issue_tracker.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * A full replacement of the details. Being a replacement rather than a patch is what makes the
 * nullable fields unambiguous: omitting {@code sprintId} means the issue leaves its sprint, because
 * the whole record is being restated rather than nudged.
 *
 * <p>Status and assignees are not here - each has its own endpoint, so the two things a board
 * changes one at a time cannot be clobbered by someone saving an edit form at the same moment. The
 * reporter is not editable at all. {@code type}, {@code priority} and {@code resolvingUnit} are
 * codes from this project's field definitions.
 */
public record IssueUpdateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description,
    @NotBlank(message = "Type cannot be blank") String type,
    @NotBlank(message = "Priority cannot be blank") String priority,
    String resolvingUnit,
    @PositiveOrZero(message = "Story point cannot be negative") Integer storyPoint,
    Integer sprintId,
    Integer epicId) {}
