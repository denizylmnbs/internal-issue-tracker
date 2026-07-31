package com.ist.internal_issue_tracker.sprint.dto;

import com.ist.internal_issue_tracker.sprint.SprintStatus;
import jakarta.validation.constraints.NotNull;

/** Shares its name with {@code project.dto.ChangeStatusRequest}; the status types differ. */
public record ChangeStatusRequest(
    @NotNull(message = "Status cannot be null") SprintStatus status) {}
