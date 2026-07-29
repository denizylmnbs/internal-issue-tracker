package com.ist.internal_issue_tracker.team.mapper;

import com.ist.internal_issue_tracker.team.Team;
import com.ist.internal_issue_tracker.team.dto.TeamCreateRequest;
import com.ist.internal_issue_tracker.team.dto.TeamResponse;
import com.ist.internal_issue_tracker.team.dto.TeamUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

  public Team toEntity(TeamCreateRequest request) {
    Team team = new Team();

    team.setName(request.name());
    team.setField(request.field());
    team.setLeaderId(request.leaderId());

    return team;
  }

  public void updateEntity(Team team, TeamUpdateRequest request) {
    team.setName(request.name());
    team.setField(request.field());
  }

  public TeamResponse toResponse(Team team) {
    return new TeamResponse(
        team.getId(),
        team.getName(),
        team.getField(),
        team.getLeaderId(),
        team.getIsActive(),
        team.getCreatedAt());
  }
}
