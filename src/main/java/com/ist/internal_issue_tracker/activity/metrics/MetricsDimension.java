package com.ist.internal_issue_tracker.activity.metrics;

/**
 * What a throughput series can be cut by.
 *
 * <p>Two constants, and the list is short on purpose. Both name a column of {@code issue_activities}
 * that {@code V3} added, and the enum is what makes it safe to let a caller choose between them -
 * the query branches on the bound value rather than interpolating a column name, so nothing outside
 * these two can ever reach the SQL.
 *
 * <p>{@code ASSIGNEE} is the obvious third and is deliberately absent, all the way down: the column
 * does not exist because {@code IssueDimensions} does not carry it, and it does not carry it because
 * of what a per-person throughput chart does to the log it is computed from. See {@code
 * IssueMetricsController}.
 */
public enum MetricsDimension {
  TYPE,
  PRIORITY
}
