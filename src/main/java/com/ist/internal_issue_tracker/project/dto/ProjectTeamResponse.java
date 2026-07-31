package com.ist.internal_issue_tracker.project.dto;

import java.time.OffsetDateTime;

/**
 * A team assigned to a project. {@code assignedAt} is the most recent assignment rather than the
 * first ever - see {@link ProjectMemberResponse}.
 */
public record ProjectTeamResponse(
    Integer id, Integer teamId, Integer projectId, Boolean isActive, OffsetDateTime assignedAt) {}
