package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamLookupAdapter implements TeamLookup {
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;

  @Override
  public boolean isLeaderOfTeam(Integer teamId, Integer userId) {
    return userId != null && teamId != null && teamRepository.existsByIdAndLeaderId(teamId, userId);
  }

  @Override
  public boolean existsActiveTeam(Integer teamId) {
    return teamId != null && teamRepository.existsByIdAndIsActiveTrue(teamId);
  }

  @Cacheable(cacheNames = "user-teams", key = "#userId", condition = "#userId != null")
  @Override
  public Set<Integer> activeTeamIdsOfUser(Integer userId) {
    return userId == null ? Set.of() : teamMemberRepository.findActiveTeamIdsByUserId(userId);
  }

  /** Not cached: only used from the write-side fan-out in {@code project}, never per-request. */
  @Override
  public Set<Integer> activeUserIdsOfTeam(Integer teamId) {
    return teamId == null ? Set.of() : teamMemberRepository.findActiveUserIdsByTeamId(teamId);
  }
}
