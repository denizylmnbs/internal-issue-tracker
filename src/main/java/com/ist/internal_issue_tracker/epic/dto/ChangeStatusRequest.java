package com.ist.internal_issue_tracker.epic.dto;

import com.ist.internal_issue_tracker.epic.EpicStatus;
import jakarta.validation.constraints.NotNull;

/** Shares its name with the project and sprint versions; the status types differ. */
public record ChangeStatusRequest(@NotNull(message = "Status cannot be null") EpicStatus status) {}
