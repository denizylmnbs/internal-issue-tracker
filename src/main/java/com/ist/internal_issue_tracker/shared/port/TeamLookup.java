package com.ist.internal_issue_tracker.shared.port;

import java.util.Set;

public interface TeamLookup {

  boolean isLeaderOfTeam(Integer teamId, Integer userId);

  /**
   * The teams a user is currently on. Exists so that {@code project} can resolve the team route into
   * a project - a user is a participant if a team they are on is assigned to it - without reading
   * {@code team_users} itself. The membership table is {@code team}'s, and a query naming it from
   * another module is a boundary violation no verification can catch, because a query is a string.
   *
   * <p>Returns ids rather than teams on purpose: the caller only ever matches them against its own
   * {@code project_teams} rows, so handing back anything richer would leak {@code team}'s shape for
   * no gain. Empty when the user is on no team - callers must handle that themselves, since an empty
   * {@code IN} list is not valid SQL.
   */
  Set<Integer> activeTeamIdsOfUser(Integer userId);

  /**
   * {@code false} if no such team exists <em>or</em> the team is soft-deleted, which callers should
   * treat the same way - a deleted team cannot be handed new work. Lets {@code project} validate a
   * team reference when assigning one to a project without ever naming a {@code team} type.
   */
  boolean existsActiveTeam(Integer teamId);
}
