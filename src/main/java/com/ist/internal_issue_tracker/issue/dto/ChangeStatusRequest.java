package com.ist.internal_issue_tracker.issue.dto;

import com.ist.internal_issue_tracker.issue.IssueStatus;
import jakarta.validation.constraints.NotNull;

/** Shares its name with the project, sprint and epic versions; the status types differ. */
public record ChangeStatusRequest(@NotNull(message = "Status cannot be null") IssueStatus status) {}
