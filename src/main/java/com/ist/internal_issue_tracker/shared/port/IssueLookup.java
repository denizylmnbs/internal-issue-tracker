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

  /**
   * The story points currently sitting in one sprint, counting live issues only and treating an
   * unestimated issue as zero. Never null - an empty sprint sums to {@code 0}, which is a real
   * answer rather than a missing one.
   *
   * <p>Exists for a single caller: {@code sprint} snapshotting what a team committed to at the
   * moment it starts a sprint. That reading has to be taken then and there, because the thing being
   * recorded is a decision and nothing later can reconstruct it.
   *
   * <p>The project is part of the question for the same reason it is on {@link
   * #existsLiveIssueInProject}: it keeps a sprint id belonging to another project from silently
   * summing that project's work.
   */
  int sumStoryPointsInSprint(Integer projectId, Integer sprintId);
}
