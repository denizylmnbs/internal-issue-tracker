package com.ist.internal_issue_tracker.epic.dto;

import java.time.OffsetDateTime;

/**
 * Only live epics are ever mapped into this record, so it carries no deletion field - a {@code
 * deletedAt} that is null on every row a caller can see tells them nothing. {@code status} is a
 * code from this project's {@code EPIC_STATUS} field definitions.
 */
public record EpicResponse(
    Integer id,
    Integer projectId,
    String name,
    String description,
    String status,
    Integer reporterId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
