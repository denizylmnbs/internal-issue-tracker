package com.ist.internal_issue_tracker.project.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ProjectResponse(
    Integer id,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    Integer leaderId,
    String status,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
