package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.project.dto.ProjectTeamCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectTeamResponse;
import com.ist.internal_issue_tracker.project.exception.ProjectNotFoundException;
import com.ist.internal_issue_tracker.project.exception.ProjectTeamErrorCode;
import com.ist.internal_issue_tracker.project.mapper.ProjectTeamMapper;
import com.ist.internal_issue_tracker.shared.event.ProjectMembershipEvent;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Teams assigned to a project. Everyone in an assigned team counts as working on the project, so
 * removing a team here quietly removes its members from that reckoning too - unless they also hold
 * a direct assignment through {@link ProjectMemberService}.
 *
 * <p>{@code actorId} is whoever is making the assignment, not anyone on the team being assigned -
 * see {@link ProjectMemberService} for the same distinction spelled out.
 */
@Service
@RequiredArgsConstructor
public class ProjectTeamService {

  private final ProjectTeamRepository projectTeamRepository;
  private final ProjectTeamMapper projectTeamMapper;
  private final ProjectRepository projectRepository;
  private final TeamLookup teamLookup;
  private final ApplicationEventPublisher eventPublisher;

  private void requireActiveProject(Integer projectId) {
    if (!projectRepository.existsByIdAndIsActiveTrue(projectId)) {
      throw new ProjectNotFoundException(projectId);
    }
  }

  /**
   * Revives an assignment that was soft-deleted, or rejects one that is still live - the mirror of
   * {@code ProjectMemberService}'s, over {@code unique_active_project_team}.
   */
  private static ProjectTeam requireInactive(ProjectTeam assignment) {
    if (Boolean.TRUE.equals(assignment.getIsActive())) {
      throw new AppException(ProjectTeamErrorCode.PROJECT_TEAM_ALREADY_EXIST);
    }

    assignment.setIsActive(true);
    return assignment;
  }

  /**
   * Putting back a team that was removed earlier reactivates its original row rather than opening a
   * second one, so a project's history stays one row per team.
   */
  @Transactional
  public ProjectTeamResponse createProjectTeam(
      Integer projectId, Integer actorId, ProjectTeamCreateRequest request) {
    // a soft-deleted project takes no new teams
    requireActiveProject(projectId);

    // the team is validated through the port, so no team type is named here
    if (!teamLookup.existsActiveTeam(request.teamId())) {
      throw new AppException(ProjectTeamErrorCode.TEAM_NOT_FOUND);
    }

    ProjectTeam projectTeam =
        projectTeamRepository
            .findFirstByProjectIdAndTeamIdOrderByIdDesc(projectId, request.teamId())
            .map(ProjectTeamService::requireInactive)
            .orElseGet(() -> projectTeamMapper.toEntity(projectId, request));

    ProjectTeam savedProjectTeam;
    try {
      savedProjectTeam = projectTeamRepository.save(projectTeam);
    } catch (DataIntegrityViolationException e) {
      // unique_active_project_team: another request assigned the same team first
      throw new AppException(ProjectTeamErrorCode.PROJECT_TEAM_ALREADY_EXIST);
    }

    eventPublisher.publishEvent(
        new ProjectMembershipEvent(
            projectId,
            request.teamId(),
            ProjectMembershipEvent.Subject.TEAM,
            ProjectMembershipEvent.Change.ADDED,
            actorId,
            OffsetDateTime.now()));

    return projectTeamMapper.toResponse(savedProjectTeam);
  }

  public PagedResponse<ProjectTeamResponse> getProjectTeams(Integer projectId, Pageable pageable) {
    requireActiveProject(projectId);

    Page<ProjectTeam> projectTeams =
        projectTeamRepository.findAllByProjectIdAndIsActiveTrue(projectId, pageable);
    Page<ProjectTeamResponse> responsePage = projectTeams.map(projectTeamMapper::toResponse);

    return PagedResponse.from(responsePage);
  }

  @Transactional
  public void removeProjectTeam(Integer projectId, Integer teamId, Integer actorId) {
    ProjectTeam assignment =
        projectTeamRepository
            .findByProjectIdAndTeamIdAndIsActiveTrue(projectId, teamId)
            .orElseThrow(() -> new AppException(ProjectTeamErrorCode.PROJECT_TEAM_NOT_FOUND));

    assignment.setIsActive(false);
    projectTeamRepository.save(assignment);

    eventPublisher.publishEvent(
        new ProjectMembershipEvent(
            projectId,
            teamId,
            ProjectMembershipEvent.Subject.TEAM,
            ProjectMembershipEvent.Change.REMOVED,
            actorId,
            OffsetDateTime.now()));
  }
}
