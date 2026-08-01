package com.ist.internal_issue_tracker.activity.metrics;

/**
 * What one sprint actually delivered. Half of velocity; the other half is the commitment, which is
 * not in this table and cannot be - see {@code Sprint#committedPoints}.
 *
 * <p>An issue counts towards the sprint its {@code DONE} row names, which is where it was when it
 * was finished rather than where it was planned. That is the right attribution and it is also the
 * one that makes carry-over visible: work planned in sprint 4 and finished in sprint 5 lands in 5,
 * so 4 misses its commitment and 5 over-delivers, which is exactly what happened.
 *
 * <p>Only the first {@code DONE} counts, so an issue reopened and re-finished in a later sprint is
 * not delivered twice.
 *
 * <p>{@code sprintId} may name a sprint that has since been soft-deleted. The service drops those
 * rather than showing a row it cannot label - see {@code IssueMetricsService#velocity}.
 */
public interface SprintVelocity {

  Integer getSprintId();

  Long getCompletedIssueCount();

  Long getCompletedPoints();
}
