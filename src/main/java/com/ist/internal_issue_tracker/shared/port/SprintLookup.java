package com.ist.internal_issue_tracker.shared.port;

import java.util.List;

public interface SprintLookup {

  /**
   * Whether the sprint exists, is not deleted, <em>and</em> belongs to that project. The project is
   * part of the question rather than a separate check: an issue may only point at a sprint of its
   * own project, so "exists" alone would let one project's issue be filed into another's sprint.
   *
   * <p>Lets {@code issue} validate a sprint reference without ever naming a {@code sprint} type.
   */
  boolean existsLiveSprintInProject(Integer projectId, Integer sprintId);

  /**
   * Every live sprint of one project, oldest start first, as the little the metrics need to know
   * about them.
   *
   * <p>Exists because velocity is half a sprint metric and half an activity one. The delivered
   * points come out of the activity log grouped by the sprint id frozen on each row; the sprint's
   * name, its dates and what was committed to it live on the sprint row and are unreachable from
   * there. This is the seam between the two, and keeping it a read-only projection is what stops the
   * {@code activity} module from acquiring a dependency on {@code sprint} to draw a chart.
   *
   * <p>A whole project's sprints at once, unpaged, rather than one lookup per row: a velocity chart
   * asks about every sprint it plots, and a project accumulates sprints at the rate of one a
   * fortnight.
   *
   * <p>Soft-deleted sprints are excluded, which means a sprint dropped after the fact takes its
   * label out of the chart while the activity rows that name it stay. The consumer decides what to
   * do about the orphan; see {@code IssueMetricsService#velocity}.
   */
  List<SprintSummary> findSprintSummaries(Integer projectId);
}
