package com.ist.internal_issue_tracker.activity.metrics;

/**
 * How much work is sitting in one status right now, and how long it has been sitting there.
 *
 * <p>Unlike every other projection here this one describes a moment rather than a window. Work in
 * progress is a level, not a flow: asking how much of it there was over the last ninety days is not
 * a question with an answer.
 *
 * <p>The age is measured from the moment the issue entered the status it is in now, so an issue
 * that has been open for a year but was picked up this morning reports as young. That is the intent
 * - the question is how long this piece of work has been stalled where it is, and the whole-life
 * version of it is lead time.
 *
 * <p>{@code storyPoints} counts an unestimated issue as zero, which makes it a floor rather than an
 * estimate. {@code issueCount} is the number to trust on a team that does not estimate everything.
 */
public interface WipStatusCount {

  String getStatus();

  Long getIssueCount();

  Long getStoryPoints();

  Double getOldestAgeSeconds();

  Double getP50AgeSeconds();
}
