package com.ist.internal_issue_tracker.issue.dto;

import jakarta.validation.constraints.NotBlank;

/** Shares its name with the project, sprint and epic versions; the status vocabularies differ. */
public record ChangeStatusRequest(@NotBlank(message = "Status cannot be blank") String status) {}
