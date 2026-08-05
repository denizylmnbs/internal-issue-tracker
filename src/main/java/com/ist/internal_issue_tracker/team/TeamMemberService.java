package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.event.TeamMembershipEvent;
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
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TeamMemberService {

  private final TeamMemberRepository teamMemberRepository;
  private final TeamMemberMapper teamMemberMapper;
  private final UserLookup userLookup;
  private final TeamRepository teamRepository;
  private final CacheManager cacheManager;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * {@code activeTeamIdsOfUser} is cached under this name, keyed by the user id alone - see {@link
   * com.ist.internal_issue_tracker.shared.cache.RedisConfig}. Evicted here rather than left to its
   * two-minute TTL because it feeds authorization decisions directly.
   */
  private void evictUserTeamsCache(Integer userId) {
    var cache = cacheManager.getCache("user-teams");
    if (cache != null) {
      cache.evict(userId);
    }
  }

  /**
   * Revives a membership that was soft-deleted, or rejects one that is still live. Removing a member
   * only clears {@code isActive}, so the row outlives them and would collide with a fresh insert on
   * {@code unique_active_team_membership} the moment they were added back.
   */
  private static TeamMember requireInactive(TeamMember membership) {
    if (Boolean.TRUE.equals(membership.getIsActive())) {
      throw new AppException(TeamMemberErrorCode.TEAM_MEMBER_ALREADY_EXIST);
    }

    membership.setIsActive(true);
    return membership;
  }

  /**
   * Adding someone who was removed earlier reactivates their original row rather than opening a
   * second one, so a team's history stays one row per person. The row keeps the {@code createdAt} of
   * their first ever join; what callers are shown as {@code joinedAt} is {@code updatedAt}, which
   * this reactivation moves to now.
   */
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

    TeamMember membership =
        teamMemberRepository
            .findFirstByTeamIdAndUserIdOrderByIdDesc(teamId, userId)
            .map(TeamMemberService::requireInactive)
            .orElseGet(() -> teamMemberMapper.toEntity(teamId, request));

    TeamMember savedTeamMember;
    try {
      savedTeamMember = teamMemberRepository.save(membership);
    } catch (DataIntegrityViolationException e) {
      // unique_active_team_membership: another request added the same user first
      throw new AppException(TeamMemberErrorCode.TEAM_MEMBER_ALREADY_EXIST);
    }

    evictUserTeamsCache(userId);
    eventPublisher.publishEvent(
        new TeamMembershipEvent(
            teamId, userId, TeamMembershipEvent.Change.ADDED, OffsetDateTime.now()));

    return teamMemberMapper.toResponse(savedTeamMember);
  }

  public PagedResponse<TeamMemberResponse> getTeamMembersByTeamId(Integer teamId, Pageable pageable) {
    if (!teamRepository.existsByIdAndIsActiveTrue(teamId)) {
      throw ResourceNotFoundException.of("Team", teamId);
    }

    Page<TeamMember> teamMembers =
        teamMemberRepository.findAllByTeamIdAndIsActiveTrue(teamId, remapJoinedAtSort(pageable));
    Page<TeamMemberResponse> responsePage = teamMembers.map(teamMemberMapper::toResponse);

    return PagedResponse.from(responsePage);
  }

  public PagedResponse<TeamMemberResponse> getAllTeamMembers(Pageable pageable) {
    Page<TeamMember> teamMembers = teamMemberRepository.findAllByIsActiveTrue(remapJoinedAtSort(pageable));
    Page<TeamMemberResponse> responsePage = teamMembers.map(teamMemberMapper::toResponse);

    return PagedResponse.from(responsePage);
  }

  /**
   * {@code TeamMemberResponse.joinedAt} is served from the entity's {@code updatedAt} ({@link
   * TeamMemberMapper#toResponse}), but the derived queries above sort directly against {@link
   * TeamMember}, which has no {@code joinedAt} property. A client sorting by the field it was shown
   * - the only field this list actually has a meaningful order on - would otherwise fail every
   * request with a {@code PropertyReferenceException}.
   */
  private static Pageable remapJoinedAtSort(Pageable pageable) {
    Sort remapped =
        Sort.by(
            pageable.getSort().stream()
                .map(
                    order ->
                        "joinedAt".equals(order.getProperty())
                            ? order.withProperty("updatedAt")
                            : order)
                .toList());

    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), remapped);
  }

  /**
   * The teams a user currently belongs to, each carrying its own name and field.
   *
   * <p>A missing user is a 404 here, not the 422 that {@code TeamMemberErrorCode.USER_NOT_FOUND}
   * carries. That code is for a user named in a <em>request body</em> - adding a member who does not
   * exist is a well-formed request pointing at something unusable. Here the user is the addressed
   * resource itself, so it answers the same way {@code GET /api/users/{id}} does, and the same way
   * {@link #getTeamMembersByTeamId} already answers for a missing team.
   */
  public PagedResponse<UserTeamMembershipResponse> getTeamsByUserId(
      Integer userId, Pageable pageable) {
    if (!userLookup.existsActiveUser(userId)) {
      throw ResourceNotFoundException.of("User", userId);
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

    evictUserTeamsCache(userId);
    eventPublisher.publishEvent(
        new TeamMembershipEvent(
            teamId, userId, TeamMembershipEvent.Change.REMOVED, OffsetDateTime.now()));
  }
}
