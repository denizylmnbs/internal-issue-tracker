package com.ist.internal_issue_tracker.epic.dto;

import com.ist.internal_issue_tracker.epic.EpicStatus;
import java.time.OffsetDateTime;

/**
 * Only live epics are ever mapped into this record, so it carries no deletion field - a {@code
 * deletedAt} that is null on every row a caller can see tells them nothing.
 */
public record EpicResponse(
    Integer id,
    Integer projectId,
    String name,
    String description,
    EpicStatus status,
    Integer reporterId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
