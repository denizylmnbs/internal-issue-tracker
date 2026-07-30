package com.ist.internal_issue_tracker.project.dto;

import com.ist.internal_issue_tracker.project.ProjectStatus;
import jakarta.validation.constraints.NotNull;

/** Moving a project through its lifecycle, mirroring {@code ChangeLeaderRequest} on teams. */
public record ChangeStatusRequest(
    @NotNull(message = "Status cannot be null") ProjectStatus status) {}
