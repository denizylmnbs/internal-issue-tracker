package com.ist.internal_issue_tracker.shared.port;

public interface TeamLookup {

  boolean isLeaderOfTeam(Integer teamId, Integer userId);

  /**
   * {@code false} if no such team exists <em>or</em> the team is soft-deleted, which callers should
   * treat the same way - a deleted team cannot be handed new work. Lets {@code project} validate a
   * team reference when assigning one to a project without ever naming a {@code team} type.
   */
  boolean existsActiveTeam(Integer teamId);
}
