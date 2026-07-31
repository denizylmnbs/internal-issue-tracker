package com.ist.internal_issue_tracker.sprint.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * A full replacement of the editable fields, which is why every one of them is required. The status
 * is not among them - it has its own endpoint, so a rename cannot quietly start a sprint.
 */
public record SprintUpdateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description,
    @NotNull(message = "Start date cannot be null") LocalDate startDate,
    LocalDate endDate) {

  @AssertTrue(message = "End date cannot be before start date")
  public boolean isEndDateNotBeforeStartDate() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
