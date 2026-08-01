package com.ist.internal_issue_tracker.activity.metrics.dto;

import com.ist.internal_issue_tracker.activity.metrics.MetricsBucket;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Bug share and defect density per bucket - see {@code DefectBucket} for why both denominators are
 * here and why neither alone is enough.
 */
public record DefectRatioResponse(
    MetricWindow window, MetricsBucket bucket, List<Point> points) {

  public record Point(
      OffsetDateTime bucketStart,
      Long createdCount,
      Long createdBugCount,
      Double createdBugShare,
      Long completedCount,
      Long completedBugCount,
      Long completedStoryPoints,
      Double defectsPerCompletedIssue,
      Double defectsPerCompletedPoint) {}
}
