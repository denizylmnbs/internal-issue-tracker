package com.ist.internal_issue_tracker.issue.dto;

import java.time.OffsetDateTime;

/**
 * Only live issues are ever mapped into this record, so it carries no deletion field.
 *
 * <p>{@code sprintId} and {@code epicId} are reported as stored. If the sprint or epic they name
 * has since been deleted, the id is still returned and a caller following it gets a 404 - soft
 * delete does not cascade here. See the note in {@code IssueService}.
 *
 * <p>{@code type}, {@code status}, {@code priority} and {@code resolvingUnit} are codes from this
 * project's field definitions.
 */
public record IssueResponse(
    Integer id,
    Integer projectId,
    Integer sprintId,
    Integer epicId,
    String type,
    String name,
    String description,
    String status,
    String priority,
    String resolvingUnit,
    Integer storyPoint,
    Integer reporterId,
    Integer assigneeUserId,
    Integer assigneeTeamId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
