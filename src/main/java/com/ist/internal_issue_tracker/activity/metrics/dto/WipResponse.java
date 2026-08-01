package com.ist.internal_issue_tracker.activity.metrics.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * What is on the board, and what has been on it too long.
 *
 * <p>Carries an {@code asOf} instant rather than a {@link MetricWindow}, because work in progress is
 * a level and not a flow - see {@code WipStatusCount}. It is the one metric here that reports the
 * present.
 *
 * <p>Two views of the same set. {@code byStatus} is the aggregate a team watches for a trend;
 * {@code oldest} is the short list it acts on, and is the only place in these metrics where an
 * individual issue is named - see {@code AgingIssue}.
 */
public record WipResponse(
    OffsetDateTime asOf, List<StatusEntry> byStatus, List<AgingEntry> oldest) {

  public record StatusEntry(
      String status,
      Long issueCount,
      Long storyPoints,
      Double oldestAgeSeconds,
      Double p50AgeSeconds) {}

  public record AgingEntry(
      Integer issueId,
      String status,
      OffsetDateTime enteredAt,
      Double ageSeconds,
      Integer storyPoint,
      String type,
      String priority) {}
}
