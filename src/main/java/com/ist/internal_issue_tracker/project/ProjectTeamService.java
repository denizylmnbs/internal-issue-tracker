package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.project.dto.ProjectTeamCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectTeamResponse;
import com.ist.internal_issue_tracker.project.exception.ProjectNotFoundException;
import com.ist.internal_issue_tracker.project.exception.ProjectTeamErrorCode;
import com.ist.internal_issue_tracker.project.mapper.ProjectTeamMapper;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Teams assigned to a project. Everyone in an assigned team counts as working on the project, so
 * removing a team here quietly removes its members from that reckoning too - unless they also hold
 * a direct assignment through {@link ProjectMemberService}.
 */
@Service
@RequiredArgsConstructor
public class ProjectTeamService {

  private final ProjectTeamRepository projectTeamRepository;
  private final ProjectTeamMapper projectTeamMapper;
  private final ProjectRepository projectRepository;
  private final TeamLookup teamLookup;

  private void requireActiveProject(Integer projectId) {
    if (!projectRepository.existsByIdAndIsActiveTrue(projectId)) {
      throw new ProjectNotFoundException(projectId);
    }
  }

  public ProjectTeamResponse createProjectTeam(
      Integer projectId, ProjectTeamCreateRequest request) {
    // a soft-deleted project takes no new teams
    requireActiveProject(projectId);

    // the team is validated through the port, so no team type is named here
    if (!teamLookup.existsActiveTeam(request.teamId())) {
      throw new AppException(ProjectTeamErrorCode.TEAM_NOT_FOUND);
    }

    ProjectTeam projectTeam = projectTeamMapper.toEntity(projectId, request);
    ProjectTeam savedProjectTeam;
    try {
      savedProjectTeam = projectTeamRepository.save(projectTeam);
    } catch (DataIntegrityViolationException e) {
      // unique_active_project_team: the team is already assigned to this project
      throw new AppException(ProjectTeamErrorCode.PROJECT_TEAM_ALREADY_EXIST);
    }

    return projectTeamMapper.toResponse(savedProjectTeam);
  }

  public PagedResponse<ProjectTeamResponse> getProjectTeams(Integer projectId, Pageable pageable) {
    requireActiveProject(projectId);

    Page<ProjectTeam> projectTeams =
        projectTeamRepository.findAllByProjectIdAndIsActiveTrue(projectId, pageable);
    Page<ProjectTeamResponse> responsePage = projectTeams.map(projectTeamMapper::toResponse);

    return PagedResponse.from(responsePage);
  }

  public void removeProjectTeam(Integer projectId, Integer teamId) {
    ProjectTeam assignment =
        projectTeamRepository
            .findByProjectIdAndTeamIdAndIsActiveTrue(projectId, teamId)
            .orElseThrow(() -> new AppException(ProjectTeamErrorCode.PROJECT_TEAM_NOT_FOUND));

    assignment.setIsActive(false);
    projectTeamRepository.save(assignment);
  }
}
