package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamLookupAdapter implements TeamLookup {
  private final TeamRepository teamRepository;

  @Override
  public boolean isLeaderOfTeam(Integer teamId, Integer userId) {
    return userId != null && teamId != null && teamRepository.existsByIdAndLeaderId(teamId, userId);
  }
}
