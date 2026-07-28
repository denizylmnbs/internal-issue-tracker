package com.ist.internal_issue_tracker.user.dto;

import com.ist.internal_issue_tracker.shared.security.Role;
import java.time.OffsetDateTime;

public record UserResponse(
    Integer id,
    String name,
    String surname,
    String email,
    Role role,
    Boolean isActive,
    OffsetDateTime createdAt) {}
