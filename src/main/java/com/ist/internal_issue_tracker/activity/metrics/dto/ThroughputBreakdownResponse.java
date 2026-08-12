package com.ist.internal_issue_tracker.activity.metrics.dto;

import com.ist.internal_issue_tracker.activity.metrics.MetricsBucket;
import com.ist.internal_issue_tracker.activity.metrics.MetricsDimension;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Throughput split by one attribute of the work - see {@code BreakdownBucket}.
 *
 * <p>The dimension is echoed back for the same reason the window is: the request may have omitted
 * it and taken the default, and a series whose meaning depends on a parameter has to say which one
 * it got.
 *
 * <p>The points are a sparse matrix, not a grid. A bucket where no bug was completed has no {@code
 * BUG} row at all rather than a zero - a client stacking these supplies its own baseline.
 */
public record ThroughputBreakdownResponse(
    MetricWindow window, MetricsBucket bucket, MetricsDimension dimension, List<Point> points) {

  public record Point(
      OffsetDateTime bucketStart, String value, Long completedCount, Long completedPoints) {}
}
