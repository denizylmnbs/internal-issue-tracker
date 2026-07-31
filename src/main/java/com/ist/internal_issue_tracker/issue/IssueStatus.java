package com.ist.internal_issue_tracker.issue;

/**
 * Where an issue stands. Mirrors the {@code CHECK} on {@code issues.status} exactly.
 *
 * <p>{@link #BACKLOG} is the starting point and has no counterpart in the sprint or epic enums: an
 * issue can exist as unplanned work long before anyone decides to do it.
 */
public enum IssueStatus {
  BACKLOG,
  TODO,
  IN_PROGRESS,
  IN_REVIEW,
  DONE,
  ON_HOLD,
  CANCELLED
}
