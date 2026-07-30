package com.ist.internal_issue_tracker.shared.port;

public interface ProjectLookup {

  boolean isLeaderOfProject(Integer projectId, Integer userId);
}
