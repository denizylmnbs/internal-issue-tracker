package com.ist.internal_issue_tracker.activity.metrics;

import java.util.Set;

/**
 * The issue statuses the metric queries reason about, and the one place their meaning is written
 * down.
 *
 * <p>These names are {@code issue.IssueStatus}'s, but {@code activity} cannot depend on {@code
 * issue} to say so - the values reach this module as strings in {@code issue_activities.new_value},
 * and the queries match on string literals. That makes the coupling real but invisible to the
 * compiler: adding a status to {@code IssueStatus} would silently fall outside every set below, and
 * flow efficiency would go on returning a number that had quietly stopped meaning what it says.
 *
 * <p>{@code MetricStatusCoverageTest} is what closes that gap. It is a test rather than production
 * code because only a test may import both enums at once - which is the same boundary that made the
 * duplication necessary in the first place.
 */
public enum MetricStatus {
  BACKLOG,
  TODO,
  IN_PROGRESS,
  IN_REVIEW,
  DONE,
  ON_HOLD,
  CANCELLED;

  /**
   * Where an issue is being worked on, as opposed to waiting. Flow efficiency is the share of an
   * issue's life spent in these, and it is the only real parameter of that metric - a team that
   * counts review as waiting will read a very different number from one that does not.
   */
  public static final Set<MetricStatus> ACTIVE = Set.of(IN_PROGRESS, IN_REVIEW);

  /**
   * What counts as delivered. {@code CANCELLED} is deliberately excluded: work that was dropped
   * left the flow but was never handed over, and counting it as throughput would let a team improve
   * its numbers by cancelling.
   */
  public static final Set<MetricStatus> COMPLETED = Set.of(DONE);
}
