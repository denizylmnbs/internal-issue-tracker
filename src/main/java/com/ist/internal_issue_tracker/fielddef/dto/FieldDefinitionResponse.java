package com.ist.internal_issue_tracker.fielddef.dto;

import java.time.OffsetDateTime;

public record FieldDefinitionResponse(
    Integer id,
    String kind,
    Integer projectId,
    String code,
    String label,
    String color,
    Integer sortOrder,
    boolean isActive,
    boolean isDefault,
    boolean isDone,
    boolean isCancelled,
    boolean isActiveWork,
    boolean isDefect,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
