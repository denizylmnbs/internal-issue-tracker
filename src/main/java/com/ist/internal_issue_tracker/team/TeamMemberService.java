package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.security.Role;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.team.dto.TeamMemberCreateRequest;
import com.ist.internal_issue_tracker.team.dto.TeamMemberResponse;
import com.ist.internal_issue_tracker.team.exception.TeamMemberErrorCode;
import com.ist.internal_issue_tracker.team.mapper.TeamMemberMapper;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TeamMemberService {

  private final TeamMemberRepository teamMemberRepository;
  private final TeamMemberMapper teamMemberMapper;
  private final UserLookup userLookup;
  private final TeamRepository teamRepository;

  public TeamMemberResponse createTeamMember(Integer teamId, TeamMemberCreateRequest request) {
    // variables
    Integer userId = request.userId();
    Role role = Role.DEVELOPER; // minimum role to become a team member

    // check team is valid
    if (!teamRepository.existsById(teamId)) {
      throw new AppException(TeamMemberErrorCode.TEAM_NOT_FOUND);
    }

    // check user is valid
    if (!userLookup.existsActiveUser(userId)) {
      throw new AppException(TeamMemberErrorCode.USER_NOT_FOUND);
    }

    // check user role
    if (!userLookup.hasAtLeastRole(userId, role)) {
      throw new AppException(TeamMemberErrorCode.USER_ROLE_NOT_ENOUGH);
    }

    TeamMember teamMember = teamMemberMapper.toEntity(teamId, request);
    TeamMember savedTeamMember;
    try {
      savedTeamMember = teamMemberRepository.save(teamMember);
    } catch (DataIntegrityViolationException e) {
      // unique_active_team_membership: the user is already an active member of this team
      throw new AppException(TeamMemberErrorCode.TEAM_MEMBER_ALREADY_EXIST);
    }

    return teamMemberMapper.toResponse(savedTeamMember);
  }

  public PagedResponse<TeamMemberResponse> getTeamMembers(Integer teamId, Pageable pageable) {
    if (!teamRepository.existsById(teamId)) {
      throw ResourceNotFoundException.of("Team", teamId);
    }

    Page<TeamMember> teamMembers = teamMemberRepository.findAllByTeamId(teamId, pageable);
    Page<TeamMemberResponse> responsePage = teamMembers.map(teamMemberMapper::toResponse);

    return PagedResponse.from(responsePage);
  }

  // Get by teamMember team id

  // Get by teams by user id

  // Soft delete team member
}
