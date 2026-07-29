package com.ist.internal_issue_tracker.user.dto;

import com.ist.internal_issue_tracker.shared.security.Role;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
        @NotNull(message = "Role cannot be null")
        Role newRole
) {}
