package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.event.TeamDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.port.FieldDefinitionLookup;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.team.dto.ChangeLeaderRequest;
import com.ist.internal_issue_tracker.team.dto.TeamCreateRequest;
import com.ist.internal_issue_tracker.team.dto.TeamResponse;
import com.ist.internal_issue_tracker.team.dto.TeamUpdateRequest;
import com.ist.internal_issue_tracker.team.exception.LeaderNotFoundException;
import com.ist.internal_issue_tracker.team.exception.TeamErrorCode;
import com.ist.internal_issue_tracker.team.exception.TeamNameAlreadyExistsException;
import com.ist.internal_issue_tracker.team.mapper.TeamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {

  private final TeamRepository teamRepository;
  private final TeamMapper teamMapper;
  private final UserLookup userLookup;
  private final FieldDefinitionLookup fieldDefinitionLookup;
  private final ApplicationEventPublisher eventPublisher;

  /** {@code field} is optional, so only a non-null value is checked against the vocabulary. */
  private void requireValidFieldIfPresent(String field) {
    if (field != null && !fieldDefinitionLookup.isValidCode(null, FieldKind.TEAM_FIELD, field)) {
      throw new AppException(TeamErrorCode.TEAM_FIELD_NOT_DEFINED);
    }
  }

  /**
   * The database only guarantees that {@code leader_id} points at an existing row; the "must still
   * be active" half of the rule has no foreign key to lean on, so it is checked here.
   */
  private void requireActiveUser(Integer leaderId) {
    if (!userLookup.existsActiveUser(leaderId)) {
      throw new LeaderNotFoundException(leaderId);
    }
  }

  /**
   * Every read and write of a single team goes through here, so a soft-deleted team is a 404 the
   * same way a never-existing one is - which is what {@link ResourceNotFoundException} already
   * promises in its own contract.
   */
  private Team requireActiveTeam(Integer id) {
    return teamRepository
        .findByIdAndIsActiveTrue(id)
        .orElseThrow(() -> ResourceNotFoundException.of("Team", id));
  }

  public TeamResponse createTeam(TeamCreateRequest request) {

    // name unique check
    if (teamRepository.existsByName(request.name())) {
      throw new TeamNameAlreadyExistsException(request.name());
    }

    // the leader must be a real, active user
    requireActiveUser(request.leaderId());
    requireValidFieldIfPresent(request.field());

    // map to entity
    Team team = teamMapper.toEntity(request);

    // save to db and prevent race conditions
    Team savedTeam;
    try {
      savedTeam = teamRepository.save(team);
    } catch (DataIntegrityViolationException e) {
      // only unique constraint on Team currently is name, revisit if a second one is added
      throw new TeamNameAlreadyExistsException(request.name());
    }

    return teamMapper.toResponse(savedTeam);
  }

  public TeamResponse getTeamById(Integer id) {
    Team team = requireActiveTeam(id);

    return teamMapper.toResponse(team);
  }

  public PagedResponse<TeamResponse> getAllTeams(
      String name, String field, Integer leaderId, Pageable pageable) {
    Page<Team> teams = teamRepository.findAllByFilters(name, field, leaderId, pageable);
    Page<TeamResponse> responsePage = teams.map(team -> teamMapper.toResponse(team));

    return PagedResponse.from(responsePage);
  }

  public TeamResponse updateTeam(Integer id, TeamUpdateRequest request) {

    // fetch existing team
    Team team = requireActiveTeam(id);

    // name unique check
    if (teamRepository.existsByNameAndIdNot(request.name(), id)) {
      throw new TeamNameAlreadyExistsException(request.name());
    }
    requireValidFieldIfPresent(request.field());

    // apply changes to the managed entity
    teamMapper.updateEntity(team, request);

    // save to db and prevent race conditions
    Team savedTeam;
    try {
      savedTeam = teamRepository.save(team);
    } catch (DataIntegrityViolationException e) {
      // only unique constraint on Team currently is name, revisit if a second one is added
      throw new TeamNameAlreadyExistsException(request.name());
    }

    return teamMapper.toResponse(savedTeam);
  }

  public TeamResponse changeLeader(Integer id, ChangeLeaderRequest request) {
    // authorization (admin-only) is enforced in SecurityConfig
    // fetch existing team
    Team team = requireActiveTeam(id);

    requireActiveUser(request.leaderId());

    team.setLeaderId(request.leaderId());

    return teamMapper.toResponse(teamRepository.save(team));
  }

  /**
   * Soft-deletes the team and retires everything hanging off it: its own roster here, and its
   * project assignments over in {@code project}. Both used to be handled by joining back to {@code
   * teams} on every read; the event moves that cost to the one moment it is actually needed.
   *
   * <p>{@code ProjectParticipantCacheEvictionListener} also reacts to this event, to evict every
   * {@code project-participant} entry this team's members held through it - and must run before
   * {@code TeamMembershipCleanupListener} and {@code ProjectAssignmentCleanupListener} retire the
   * rows it reads to find them, which is why it is the one {@code @Order}-annotated listener on
   * this event.
   */
  @Transactional
  public void deleteTeam(Integer id) {
    // fetch existing team
    Team team = requireActiveTeam(id);

    team.setIsActive(false);

    teamRepository.save(team);

    eventPublisher.publishEvent(new TeamDeactivatedEvent(id));
  }
}
