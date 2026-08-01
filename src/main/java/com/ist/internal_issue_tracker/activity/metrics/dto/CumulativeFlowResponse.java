package com.ist.internal_issue_tracker.activity.metrics.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A cumulative flow diagram as data: one row per day per occupied status - see {@code CfdPoint}.
 *
 * <p>Bucketing is fixed at a day and there is no {@code bucket} parameter. A CFD is read for the
 * width of its bands over time, and a weekly or monthly cut smooths away the queue that forming is
 * the only thing it is drawn to show.
 */
public record CumulativeFlowResponse(MetricWindow window, List<Point> points) {

  public record Point(OffsetDateTime bucketStart, String status, Long issueCount) {}
}
