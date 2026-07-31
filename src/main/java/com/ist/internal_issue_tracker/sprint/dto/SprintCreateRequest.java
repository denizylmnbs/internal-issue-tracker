package com.ist.internal_issue_tracker.sprint.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The project is not a field here - it comes from the path, so there is only one place it can be
 * given and no way for the two to disagree.
 *
 * <p>The status is deliberately absent for the same reason it is on {@code ProjectCreateRequest}:
 * every sprint starts in {@code TODO} and moves on through its own endpoint, so a caller cannot open
 * one straight into {@code IN_PROGRESS} and slip past the "one running sprint per project" check.
 */
public record SprintCreateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description,
    @NotNull(message = "Start date cannot be null") LocalDate startDate,
    LocalDate endDate) {

  /** Cross-field rule - see {@code ProjectCreateRequest#isEndDateNotBeforeStartDate}. */
  @AssertTrue(message = "End date cannot be before start date")
  public boolean isEndDateNotBeforeStartDate() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
