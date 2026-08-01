package com.ist.internal_issue_tracker.activity.metrics;

import java.time.Instant;

/**
 * Throughput for one bucket and one value of whichever dimension was asked for - see {@link
 * MetricsDimension}.
 *
 * <p>The dimension arrives as text rather than as a column of its own because the query serves both
 * cuts. That is not a shortcut: the two are the same question asked of a different attribute, and
 * duplicating a page of SQL to change one expression is how the two versions drift apart.
 *
 * <p>{@code dimensionValue} is never null. An issue with no type set reports as {@code UNSET}, which
 * is a real category and often a large one - a plain null would be dropped by most charting code and
 * the series would silently stop adding up to the total.
 *
 * <p>The value read is the one frozen on the row where the issue reached {@code DONE}, so an issue
 * re-typed from {@code TASK} to {@code BUG} after delivery stays counted as what it was when it
 * shipped.
 */
public interface BreakdownBucket {

  Instant getBucketStart();

  String getDimensionValue();

  Long getCompletedCount();

  Long getCompletedPoints();
}
