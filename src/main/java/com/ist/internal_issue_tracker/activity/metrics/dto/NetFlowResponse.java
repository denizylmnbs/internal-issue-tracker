package com.ist.internal_issue_tracker.activity.metrics.dto;

import com.ist.internal_issue_tracker.activity.metrics.MetricsBucket;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Arrivals against departures, and the backlog trend that falls out of the difference - see {@code
 * NetFlowBucket}.
 */
public record NetFlowResponse(MetricWindow window, MetricsBucket bucket, List<Point> points) {

  public record Point(
      OffsetDateTime bucketStart,
      Long createdCount,
      Long completedCount,
      Long netCount,
      Long cumulativeNetCount) {}
}
