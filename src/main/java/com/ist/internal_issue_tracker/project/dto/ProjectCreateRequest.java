package com.ist.internal_issue_tracker.project.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The status is deliberately absent: every project starts in {@code PLANNING} and moves on through
 * its own endpoint, so a caller cannot open a project straight into {@code COMPLETED}.
 *
 * <p>{@code leaderId} is optional - a project can be opened now and staffed later. When it is given
 * it still has to name an active user.
 */
public record ProjectCreateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description,
    @NotNull(message = "Start date cannot be null") LocalDate startDate,
    LocalDate endDate,
    Integer leaderId) {

  /**
   * Cross-field rule, so it cannot live on either component alone. It passes when a date is missing
   * - the {@code @NotNull} above already reports that, and reporting it twice would only muddy the
   * response.
   */
  @AssertTrue(message = "End date cannot be before start date")
  public boolean isEndDateNotBeforeStartDate() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
