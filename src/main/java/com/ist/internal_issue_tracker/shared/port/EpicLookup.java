package com.ist.internal_issue_tracker.shared.port;

public interface EpicLookup {

  /**
   * Whether the epic exists, is not deleted, <em>and</em> belongs to that project - the mirror of
   * {@link SprintLookup#existsLiveSprintInProject}, and for the same reason.
   */
  boolean existsLiveEpicInProject(Integer projectId, Integer epicId);
}
