package com.ist.internal_issue_tracker.issue.dto;

import com.ist.internal_issue_tracker.issue.IssuePriority;
import com.ist.internal_issue_tracker.issue.IssueStatus;
import com.ist.internal_issue_tracker.issue.IssueType;
import com.ist.internal_issue_tracker.issue.IssueUnit;
import java.time.OffsetDateTime;

/**
 * Only live issues are ever mapped into this record, so it carries no deletion field.
 *
 * <p>{@code sprintId} and {@code epicId} are reported as stored. If the sprint or epic they name has
 * since been deleted, the id is still returned and a caller following it gets a 404 - soft delete
 * does not cascade here. See the note in {@code IssueService}.
 */
public record IssueResponse(
    Integer id,
    Integer projectId,
    Integer sprintId,
    Integer epicId,
    IssueType type,
    String name,
    String description,
    IssueStatus status,
    IssuePriority priority,
    IssueUnit resolvingUnit,
    Integer storyPoint,
    Integer reporterId,
    Integer assigneeUserId,
    Integer assigneeTeamId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
