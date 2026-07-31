package com.ist.internal_issue_tracker.shared.port;

public interface IssueLookup {

  /**
   * Whether the issue exists, is not deleted, <em>and</em> belongs to that project - the same shape
   * as {@link SprintLookup#existsLiveSprintInProject} and for the same reason.
   *
   * <p>Note what it does <em>not</em> answer: an issue row does not know whether its project has
   * been soft-deleted, so a live issue on a dead project still returns {@code true} here. Callers
   * check the project separately and first; see {@code CommentService}.
   */
  boolean existsLiveIssueInProject(Integer projectId, Integer issueId);
}
