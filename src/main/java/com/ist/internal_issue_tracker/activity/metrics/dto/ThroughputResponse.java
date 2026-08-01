package com.ist.internal_issue_tracker.activity.metrics.dto;

import com.ist.internal_issue_tracker.activity.metrics.MetricsBucket;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Issues completed per bucket. Buckets with nothing in them are absent rather than zero - see
 * {@code ThroughputBucket}.
 */
public record ThroughputResponse(
    MetricWindow window, MetricsBucket bucket, List<Point> points) {

  public record Point(OffsetDateTime bucketStart, Long completedCount) {}
}
