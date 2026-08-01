package com.ist.internal_issue_tracker.activity.metrics;

import java.time.Instant;

/**
 * One piece of in-flight work and how long it has been standing still.
 *
 * <p>The single named exception to the rule that these metrics stop at the project. It has to be:
 * "the board has four items older than three weeks" is not actionable, and "these four" is. What
 * keeps it from becoming a performance measure is that it names issues and not people - there is no
 * assignee here, and {@code IssueDimensions} explains why there is none anywhere.
 *
 * <p>{@code BACKLOG} is excluded by the query that fills this. An untouched backlog item is old by
 * definition and would crowd out everything worth looking at; aging applies to work that has been
 * started and then left.
 *
 * <p>{@code Instant} rather than {@code OffsetDateTime} - see {@code ThroughputBucket}.
 */
public interface AgingIssue {

  Integer getIssueId();

  String getStatus();

  Instant getEnteredAt();

  Double getAgeSeconds();

  Integer getStoryPoint();

  String getIssueType();

  String getPriority();
}
