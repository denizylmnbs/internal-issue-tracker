package com.ist.internal_issue_tracker.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token cannot be blank") String refreshToken) {}
