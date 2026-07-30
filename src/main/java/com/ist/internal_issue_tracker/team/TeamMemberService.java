package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.security.Role;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.team.dto.TeamMemberCreateRequest;
import com.ist.internal_issue_tracker.team.dto.TeamMemberResponse;
import com.ist.internal_issue_tracker.team.dto.UserTeamMembershipResponse;
import com.ist.internal_issue_tracker.team.exception.TeamMemberErrorCode;
import com.ist.internal_issue_tracker.team.mapper.TeamMemberMapper;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // check team is valid - a soft-deleted team takes no new members
    if (!teamRepository.existsByIdAndIsActiveTrue(teamId)) {
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

  public PagedResponse<TeamMemberResponse> getTeamMembersByTeamId(Integer teamId, Pageable pageable) {
    if (!teamRepository.existsByIdAndIsActiveTrue(teamId)) {
      throw ResourceNotFoundException.of("Team", teamId);
    }

    Page<TeamMember> teamMembers =
        teamMemberRepository.findAllByTeamIdAndIsActiveTrue(teamId, pageable);
    Page<TeamMemberResponse> responsePage = teamMembers.map(teamMemberMapper::toResponse);

    return PagedResponse.from(responsePage);
  }

  public PagedResponse<TeamMemberResponse> getAllTeamMembers(Pageable pageable) {
    Page<TeamMember> teamMembers = teamMemberRepository.findAllByIsActiveTrue(pageable);
    Page<TeamMemberResponse> responsePage = teamMembers.map(teamMemberMapper::toResponse);

    return PagedResponse.from(responsePage);
  }

  /** The teams a user currently belongs to, each carrying its own name and field. */
  public PagedResponse<UserTeamMembershipResponse> getTeamsByUserId(
      Integer userId, Pageable pageable) {
    if (!userLookup.existsActiveUser(userId)) {
      throw new AppException(TeamMemberErrorCode.USER_NOT_FOUND);
    }

    Page<UserTeamMembershipResponse> memberships =
        teamMemberRepository.findActiveMembershipsWithTeamByUserId(userId, pageable);

    return PagedResponse.from(memberships);
  }

  /**
   * Soft delete: the row stays for history and only {@code isActive} is cleared. The entity is
   * loaded and mutated rather than updated through a bulk {@code @Modifying} query so that dirty
   * checking still fires {@code @UpdateTimestamp} - a JPQL update would leave {@code updatedAt}
   * stale.
   */
  public void removeTeamMember(Integer teamId, Integer userId) {
    TeamMember membership =
        teamMemberRepository
            .findByTeamIdAndUserIdAndIsActiveTrue(teamId, userId)
            .orElseThrow(() -> new AppException(TeamMemberErrorCode.TEAM_MEMBER_NOT_FOUND));

    membership.setIsActive(false);
    teamMemberRepository.save(membership);
  }
}
