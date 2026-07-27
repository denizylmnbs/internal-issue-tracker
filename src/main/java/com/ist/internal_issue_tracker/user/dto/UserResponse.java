package com.ist.internal_issue_tracker.user.dto;

import java.time.OffsetDateTime;

public record UserResponse(
    Integer id,
    String name,
    String surname,
    String email,
    Boolean isAdmin,
    Boolean isActive,
    OffsetDateTime createdAt) {}
