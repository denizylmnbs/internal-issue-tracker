package com.ist.internal_issue_tracker.project.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Moving a project through its lifecycle, mirroring {@code ChangeLeaderRequest} on teams. {@code
 * status} is a code from the global {@code PROJECT_STATUS} field definitions.
 */
public record ChangeStatusRequest(@NotBlank(message = "Status cannot be blank") String status) {}
