package com.ist.internal_issue_tracker.activity.metrics.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * What each sprint promised and what it delivered.
 *
 * <p>No {@link MetricWindow}: a sprint is its own window, and the series is every sprint the
 * project has run in the order it ran them. Sprints that have not started yet are included with
 * nulls, which is how a client can chart the plan alongside the history.
 *
 * <p>{@code committedPoints} may be null while {@code completedPoints} is not - a sprint that
 * predates the commitment column, or one whose work was finished without the sprint ever being
 * started. {@code sayDoRatio} is then null too rather than being invented from the delivered
 * figure, which would make every such sprint look like a perfect hit.
 *
 * <p>A ratio above 1.0 is not an error and not necessarily good: it means more was delivered than
 * was committed, which is usually scope pulled in mid-sprint and sometimes an under-ambitious plan.
 */
public record VelocityResponse(List<Sprint> sprints) {

  public record Sprint(
      Integer sprintId,
      String name,
      String status,
      LocalDate startDate,
      LocalDate endDate,
      Integer committedPoints,
      Long completedPoints,
      Long completedIssueCount,
      Double sayDoRatio) {}
}
