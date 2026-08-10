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

  /**
   * {@code updatedAt} is nullable (no default, unlike {@code createdAt}) for any row that has never
   * gone through Hibernate's {@code @UpdateTimestamp} path - e.g. one inserted by hand. Falling back
   * to {@code createdAt} there is not a guess: for a membership never touched since it was created,
   * "joined" and "created" are the same moment.
   */
  public TeamMemberResponse toResponse(TeamMember teamMember) {
    return new TeamMemberResponse(
        teamMember.getId(),
        teamMember.getUserId(),
        teamMember.getTeamId(),
        teamMember.getIsActive(),
        teamMember.getUpdatedAt() != null ? teamMember.getUpdatedAt() : teamMember.getCreatedAt());
  }
}
