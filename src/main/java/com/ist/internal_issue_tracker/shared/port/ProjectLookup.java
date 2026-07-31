package com.ist.internal_issue_tracker.shared.port;

public interface ProjectLookup {

  boolean isLeaderOfProject(Integer projectId, Integer userId);

  /**
   * {@code false} if no such project exists <em>or</em> the project is soft-deleted, which callers
   * should treat the same way - a deleted project takes no new work. Lets {@code sprint} validate
   * the project a sprint hangs off without ever naming a {@code project} type.
   */
  boolean existsActiveProject(Integer projectId);
}
