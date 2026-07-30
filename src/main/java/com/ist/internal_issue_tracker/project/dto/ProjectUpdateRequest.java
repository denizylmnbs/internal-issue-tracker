package com.ist.internal_issue_tracker.project.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Full replacement of a project's own details. Leader and status are deliberately absent - each is
 * its own operation, the same way {@code TeamUpdateRequest} leaves the leader out.
 */
public record ProjectUpdateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description,
    @NotNull(message = "Start date cannot be null") LocalDate startDate,
    LocalDate endDate) {

  @AssertTrue(message = "End date cannot be before start date")
  public boolean isEndDateNotBeforeStartDate() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
