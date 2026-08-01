package com.ist.internal_issue_tracker.activity.metrics.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * One sprint's burndown, replayed day by day from the activity log - see {@code BurndownPoint}.
 *
 * <p>The sprint's own figures come back alongside the series because the chart is unreadable without
 * them: the ideal line runs from {@code committedPoints} at {@code startDate} to zero at {@code
 * endDate}, and it is drawn client-side from exactly these three fields.
 *
 * <p>The series stops at the sprint's end date or at today, whichever is earlier, so a running sprint
 * does not trail a flat line into the future.
 */
public record BurndownResponse(
    Integer sprintId,
    String name,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    Integer committedPoints,
    List<Point> points) {

  public record Point(
      OffsetDateTime bucketStart,
      Long remainingPoints,
      Long remainingIssueCount,
      Long completedPoints,
      Long scopePoints) {}
}
