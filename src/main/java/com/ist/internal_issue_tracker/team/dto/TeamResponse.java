package com.ist.internal_issue_tracker.team.dto;

import java.time.OffsetDateTime;

public record TeamResponse(
    Integer id,
    String name,
    String field,
    Integer leaderId,
    Boolean isActive,
    OffsetDateTime createdAt) {}
