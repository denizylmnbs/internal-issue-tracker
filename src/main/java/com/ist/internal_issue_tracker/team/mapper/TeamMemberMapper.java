package com.ist.internal_issue_tracker.team.mapper;

import com.ist.internal_issue_tracker.team.TeamMember;
import com.ist.internal_issue_tracker.team.dto.TeamMemberCreateRequest;
import com.ist.internal_issue_tracker.team.dto.TeamMemberResponse;
import org.springframework.stereotype.Component;

@Component
public class TeamMemberMapper {
  public TeamMember toEntity(Integer teamId, TeamMemberCreateRequest request) {
    TeamMember teamMember = new TeamMember();

    teamMember.setTeamId(teamId);
    teamMember.setUserId(request.userId());

    return teamMember;
  }

  public TeamMemberResponse toResponse(TeamMember teamMember) {
    return new TeamMemberResponse(
        teamMember.getId(),
        teamMember.getUserId(),
        teamMember.getTeamId(),
        teamMember.getIsActive());
  }
}
