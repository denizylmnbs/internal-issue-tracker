package com.ist.internal_issue_tracker.issue;

/**
 * One project's one sprint, summed for a single assignee: what they were carrying and how much of
 * it they finished, read from {@code issues}' current state rather than the activity log.
 *
 * <p>This is deliberately not the same number {@code IssueMetricsService#velocity} reports for the
 * sprint as a whole - that one replays {@code issue_activities} for the point-in-time story point
 * an issue had when it was marked {@code DONE}, and that table carries no assignee. Reading current
 * state instead means a completed issue that is later re-estimated or moved to another sprint shows
 * up here retroactively, which the activity log would not do. Acceptable for a personal progress
 * summary; not a substitute for the project's velocity chart.
 */
public interface UserSprintPoints {
  Integer getProjectId();

  Integer getSprintId();

  Long getAssignedPoints();

  Long getCompletedPoints();

  Long getAssignedIssueCount();

  Long getCompletedIssueCount();
}
