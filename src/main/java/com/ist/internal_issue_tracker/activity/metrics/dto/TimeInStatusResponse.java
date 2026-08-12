package com.ist.internal_issue_tracker.activity.metrics.dto;

import java.util.List;

/**
 * Where the project's time went, one entry per status. Read across the entries it locates the queue
 * - see {@code StatusDuration}.
 */
public record TimeInStatusResponse(MetricWindow window, List<Entry> entries) {

  public record Entry(String status, Long issueCount, Double totalSeconds, Double p50Seconds) {}
}
