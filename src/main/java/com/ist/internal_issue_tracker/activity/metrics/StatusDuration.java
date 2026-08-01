package com.ist.internal_issue_tracker.activity.metrics;

/**
 * How long the project's issues spent in one status, within the window.
 *
 * <p>Read as a whole it is where work waits. A large total against {@code IN_REVIEW} with a small
 * one against {@code IN_PROGRESS} says the bottleneck is review rather than development, which is
 * the kind of thing no status board shows because a board only knows about now.
 *
 * <p>{@code getStatus} is a String rather than {@link MetricStatus}: it is whatever the activity log
 * recorded, and a status that has since been removed from the application still appears in history.
 * Parsing it here would throw on exactly the rows most worth seeing.
 */
public interface StatusDuration {

  String getStatus();

  Long getIssueCount();

  Double getTotalSeconds();

  Double getP50Seconds();
}
