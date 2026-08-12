package com.ist.internal_issue_tracker.epic.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Shares its name with the project and sprint versions. {@code status} is a code from this
 * project's {@code EPIC_STATUS} field definitions - {@code EpicService} validates it, not a fixed
 * enum here.
 */
public record ChangeStatusRequest(@NotBlank(message = "Status cannot be blank") String status) {}
