package com.ist.internal_issue_tracker.shared.port;

public interface SprintLookup {

  /**
   * Whether the sprint exists, is not deleted, <em>and</em> belongs to that project. The project is
   * part of the question rather than a separate check: an issue may only point at a sprint of its
   * own project, so "exists" alone would let one project's issue be filed into another's sprint.
   *
   * <p>Lets {@code issue} validate a sprint reference without ever naming a {@code sprint} type.
   */
  boolean existsLiveSprintInProject(Integer projectId, Integer sprintId);
}
