package com.ist.internal_issue_tracker.project.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeLeaderRequest(
    @NotNull(message = "Leader id cannot be null") Integer leaderId) {}
