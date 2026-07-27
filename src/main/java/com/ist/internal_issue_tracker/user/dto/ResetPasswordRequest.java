package com.ist.internal_issue_tracker.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "New password cannot be blank")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword) {}
