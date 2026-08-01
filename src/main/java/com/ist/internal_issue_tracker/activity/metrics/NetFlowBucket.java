package com.ist.internal_issue_tracker.activity.metrics;

import java.time.Instant;

/**
 * What came in and what went out in one bucket, and where that leaves the pile.
 *
 * <p>The metric that answers the question throughput on its own cannot: a team completing twelve
 * issues a week looks healthy until you notice fifteen a week are arriving. Sustained positive
 * {@code netCount} means the backlog is growing no matter how fast anything moves, and no amount of
 * cycle time improvement will fix it - it is a demand problem, not a flow problem.
 *
 * <p>{@code cumulativeNetCount} runs from the start of the requested window, not from the start of
 * the project. It is the shape of the line that carries the meaning, so its offset does not matter;
 * treating it as the real backlog size would be wrong.
 *
 * <p>Both sides count issues rather than points. Created work is usually unestimated at the moment
 * it arrives, so a points version of this would compare a number that is mostly zero against one
 * that is not.
 */
public interface NetFlowBucket {

  Instant getBucketStart();

  Long getCreatedCount();

  Long getCompletedCount();

  Long getNetCount();

  Long getCumulativeNetCount();
}
