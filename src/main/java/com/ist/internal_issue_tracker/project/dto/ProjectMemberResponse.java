package com.ist.internal_issue_tracker.project.dto;

import java.time.OffsetDateTime;

/**
 * A direct assignment seen from the project's side. {@code joinedAt} is the most recent assignment
 * rather than the first ever: removing someone only clears {@code isActive} and adding them back
 * revives that same row, so the row's creation date would report a stint they have since left.
 */
public record ProjectMemberResponse(
    Integer id, Integer userId, Integer projectId, Boolean isActive, OffsetDateTime joinedAt) {}
