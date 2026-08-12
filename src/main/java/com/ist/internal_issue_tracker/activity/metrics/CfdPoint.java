package com.ist.internal_issue_tracker.activity.metrics;

import java.time.Instant;

/**
 * How many issues stood in one status at the end of one day - a cumulative flow diagram, one cell
 * at a time.
 *
 * <p>The chart worth having when a single number will not do. Stacked, the bands' <em>widths</em>
 * are the queues: a band that thickens over time is a status work enters faster than it leaves, and
 * that is where the constraint is. Time in status says the same thing as a total; this says when it
 * started.
 *
 * <p>One row per day per occupied status. Statuses with nobody in them that day are absent rather
 * than zero, for the reason on {@code ThroughputBucket} - a client stacking bands supplies its own
 * zero.
 *
 * <p>Deleted issues drop out entirely, on every day including the ones before they were deleted.
 * That is a deliberate difference from the burndown, which shows them leaving: a CFD is read for
 * the shape of its bands rather than for its totals, and an issue that no longer exists distorts
 * the shape more than it informs it.
 */
public interface CfdPoint {

  Instant getBucketStart();

  String getStatus();

  Long getIssueCount();
}
