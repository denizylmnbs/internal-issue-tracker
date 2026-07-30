package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.project.dto.ChangeLeaderRequest;
import com.ist.internal_issue_tracker.project.dto.ChangeStatusRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectDetailResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectUpdateRequest;
import com.ist.internal_issue_tracker.project.exception.ProjectLeaderNotFoundException;
import com.ist.internal_issue_tracker.project.exception.ProjectNameAlreadyExistsException;
import com.ist.internal_issue_tracker.project.exception.ProjectNotFoundException;
import com.ist.internal_issue_tracker.project.mapper.ProjectMapper;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMapper projectMapper;
  private final UserLookup userLookup;

  /**
   * The database only guarantees that {@code leader_id} points at an existing row; the "must still
   * be active" half of the rule has no foreign key to lean on, so it is checked here.
   */
  private void requireActiveUser(Integer leaderId) {
    if (!userLookup.existsActiveUser(leaderId)) {
      throw new ProjectLeaderNotFoundException(leaderId);
    }
  }

  /**
   * Every read and write of a single project goes through here, so a soft-deleted project is a 404
   * the same way a never-existing one is.
   */
  private Project requireActiveProject(Integer id) {
    return projectRepository
        .findByIdAndIsActiveTrue(id)
        .orElseThrow(() -> new ProjectNotFoundException(id));
  }

  public ProjectResponse createProject(ProjectCreateRequest request) {

    // name unique check
    if (projectRepository.existsByName(request.name())) {
      throw new ProjectNameAlreadyExistsException(request.name());
    }

    // a leader is optional here, but if one is named it must be a real, active user
    if (request.leaderId() != null) {
      requireActiveUser(request.leaderId());
    }

    // map to entity - the status is left at the entity's PLANNING default
    Project project = projectMapper.toEntity(request);

    // save to db and prevent race conditions
    Project savedProject;
    try {
      savedProject = projectRepository.save(project);
    } catch (DataIntegrityViolationException e) {
      // only unique constraint on Project currently is name, revisit if a second one is added
      throw new ProjectNameAlreadyExistsException(request.name());
    }

    return projectMapper.toResponse(savedProject);
  }

  /** The detail view, which is the only place the member and team rollups are worth paying for. */
  public ProjectDetailResponse getProjectById(Integer id) {
    Project project = requireActiveProject(id);

    return projectMapper.toDetailResponse(
        project,
        projectRepository.countActiveMembers(id),
        projectRepository.countActiveTeams(id));
  }

  public PagedResponse<ProjectResponse> getAllProjects(
      String name,
      ProjectStatus status,
      Integer leaderId,
      LocalDate startDateAfter,
      LocalDate endDateBefore,
      Pageable pageable) {
    Page<Project> projects =
        projectRepository.findAllByFilters(
            name, status, leaderId, startDateAfter, endDateBefore, pageable);
    Page<ProjectResponse> responsePage = projects.map(projectMapper::toResponse);

    return PagedResponse.from(responsePage);
  }

  public ProjectResponse updateProject(Integer id, ProjectUpdateRequest request) {

    // fetch existing project
    Project project = requireActiveProject(id);

    // name unique check
    if (projectRepository.existsByNameAndIdNot(request.name(), id)) {
      throw new ProjectNameAlreadyExistsException(request.name());
    }

    // apply changes to the managed entity
    projectMapper.updateEntity(project, request);

    // save to db and prevent race conditions
    Project savedProject;
    try {
      savedProject = projectRepository.save(project);
    } catch (DataIntegrityViolationException e) {
      // only unique constraint on Project currently is name, revisit if a second one is added
      throw new ProjectNameAlreadyExistsException(request.name());
    }

    return projectMapper.toResponse(savedProject);
  }

  public ProjectResponse changeLeader(Integer id, ChangeLeaderRequest request) {
    // authorization (editor-only) is enforced in SecurityConfig
    Project project = requireActiveProject(id);

    requireActiveUser(request.leaderId());

    project.setLeaderId(request.leaderId());

    return projectMapper.toResponse(projectRepository.save(project));
  }

  /**
   * Hands the project back to nobody. Allowed because a project is permitted to exist without a
   * leader at all - the same state it can be created in - and forcing a replacement would mean a
   * departing leader has to be swapped for an arbitrary stand-in.
   */
  public ProjectResponse removeLeader(Integer id) {
    Project project = requireActiveProject(id);

    project.setLeaderId(null);

    return projectMapper.toResponse(projectRepository.save(project));
  }

  /**
   * Any status may follow any other - a project can be reopened out of {@code COMPLETED} or {@code
   * CANCELLED}. If that ever needs narrowing, this is the one place it has to happen.
   */
  public ProjectResponse changeStatus(Integer id, ChangeStatusRequest request) {
    Project project = requireActiveProject(id);

    project.setStatus(request.status());

    return projectMapper.toResponse(projectRepository.save(project));
  }

  public void deleteProject(Integer id) {
    Project project = requireActiveProject(id);

    project.setIsActive(false);

    projectRepository.save(project);
  }
}
