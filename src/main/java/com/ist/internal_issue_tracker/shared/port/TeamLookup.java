package com.ist.internal_issue_tracker.shared.port;

public interface TeamLookup {

  boolean isLeaderOfTeam(Integer teamId, Integer userId);
}
