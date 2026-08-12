package com.ist.internal_issue_tracker.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String field,
    @NotNull(message = "Leader id cannot be null") Integer leaderId) {}
