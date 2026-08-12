package com.ist.internal_issue_tracker.activity.metrics;

/**
 * How often work called finished had to be picked up again - the closest thing this log holds to a
 * quality signal.
 *
 * <p>An issue counts once however many times it was reopened, so the rate is "what share of
 * finished work came back" rather than a count of bounces. Both readings are defensible; this one
 * is the one that stays between 0 and 1 and can be compared across windows.
 */
public interface ReopenStats {

  Long getDoneIssueCount();

  Long getReopenedIssueCount();

  Double getReopenRate();
}
