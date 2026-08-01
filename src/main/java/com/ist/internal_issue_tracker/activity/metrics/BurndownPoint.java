package com.ist.internal_issue_tracker.activity.metrics;

import java.time.Instant;

/**
 * One day of a sprint burndown, replayed from the activity log rather than recorded at the time.
 *
 * <p>That replay is what makes {@code scopePoints} worth as much as {@code remainingPoints}. A
 * burndown drawn from a snapshot taken each night can only show the line going down; one
 * reconstructed from the log knows when work was <em>added</em>, so a flat line caused by the team
 * delivering exactly as much as was pushed in is distinguishable from a flat line caused by nothing
 * happening. Those look identical on most burndown charts and mean opposite things.
 *
 * <p>Membership is as-of, not current: an issue counts towards this sprint on the days its latest
 * activity said it was in it. Moving an issue out tomorrow leaves yesterday's chart alone, which is
 * the whole reason {@code issue_activities.sprint_id} is frozen per row.
 *
 * <p>Cancelled and deleted issues leave scope on the day they go, rather than sitting in the
 * remainder forever. Dropping work is a legitimate way to finish a sprint and the chart should show
 * it as scope falling, not as work completing.
 *
 * <p>No ideal line is computed here. It is a straight line from the commitment to zero across the
 * sprint's dates, all three of which come back on the response, and drawing it is the client's job.
 */
public interface BurndownPoint {

  Instant getBucketStart();

  Long getRemainingPoints();

  Long getRemainingIssueCount();

  Long getCompletedPoints();

  Long getScopePoints();
}
