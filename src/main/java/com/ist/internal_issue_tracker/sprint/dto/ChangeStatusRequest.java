package com.ist.internal_issue_tracker.sprint.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Shares its name with {@code project.dto.ChangeStatusRequest}. {@code status} is a code from
 * this project's {@code SPRINT_STATUS} field definitions - {@code SprintService} validates it.
 */
public record ChangeStatusRequest(@NotBlank(message = "Status cannot be blank") String status) {}
