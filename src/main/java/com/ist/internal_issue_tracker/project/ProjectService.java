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
import com.ist.internal_issue_tracker.shared.event.ProjectChangedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectCreatedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectDeletedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectFieldChange;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code actorId} on every write is whoever is making the change, bound for {@code
 * project_activities.user_id}. It is not the leader and not a member - see {@code IssueService} for
 * the full reasoning.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectTeamRepository projectTeamRepository;
  private final ProjectMapper projectMapper;
  private final ProjectChangeDetector projectChangeDetector;
  private final UserLookup userLookup;
  private final ApplicationEventPublisher eventPublisher;
  private final CacheManager cacheManager;

  /**
   * {@code isLeaderOfProject} is cached under this name, keyed by {@code projectId + ':' + userId}
   * - see {@code ProjectLookupAdapter}. A leader change touches at most two keys (who it was, who
   * it is now), both already in hand at the call site, so this evicts exactly them rather than
   * waiting on the two-minute TTL.
   */
  private void evictLeaderCache(Integer projectId, Integer userId) {
    if (userId == null) {
      return;
    }

    Cache cache = cacheManager.getCache("project-leader");
    if (cache != null) {
      cache.evict(projectId + ":" + userId);
    }
  }

  /** Publishes only if something moved - see {@code IssueService#publishChanges}. */
  private void publishChanges(Integer actorId, ProjectSnapshot before, Project after) {
    List<ProjectFieldChange> changes = projectChangeDetector.diff(before, after);

    if (changes.isEmpty()) {
      return;
    }

    eventPublisher.publishEvent(
        new ProjectChangedEvent(after.getId(), actorId, OffsetDateTime.now(), changes));
  }

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

  @Transactional
  public ProjectResponse createProject(Integer actorId, ProjectCreateRequest request) {

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

    eventPublisher.publishEvent(
        new ProjectCreatedEvent(savedProject.getId(), actorId, OffsetDateTime.now()));

    return projectMapper.toResponse(savedProject);
  }

  /** The detail view, which is the only place the member and team rollups are worth paying for. */
  public ProjectDetailResponse getProjectById(Integer id) {
    Project project = requireActiveProject(id);

    return projectMapper.toDetailResponse(
        project,
        projectMemberRepository.countActiveMembers(id),
        projectTeamRepository.countByProjectIdAndIsActiveTrue(id));
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

  @Transactional
  public ProjectResponse updateProject(Integer id, Integer actorId, ProjectUpdateRequest request) {

    // fetch existing project
    Project project = requireActiveProject(id);

    // name unique check
    if (projectRepository.existsByNameAndIdNot(request.name(), id)) {
      throw new ProjectNameAlreadyExistsException(request.name());
    }

    // before updateEntity, which is what makes the old values still readable
    ProjectSnapshot before = ProjectSnapshot.of(project);

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

    publishChanges(actorId, before, savedProject);

    return projectMapper.toResponse(savedProject);
  }

  @Transactional
  public ProjectResponse changeLeader(Integer id, Integer actorId, ChangeLeaderRequest request) {
    // authorization (editor-only) is enforced in SecurityConfig
    Project project = requireActiveProject(id);

    requireActiveUser(request.leaderId());

    ProjectSnapshot before = ProjectSnapshot.of(project);

    project.setLeaderId(request.leaderId());

    Project savedProject = projectRepository.save(project);

    evictLeaderCache(id, before.leaderId());
    evictLeaderCache(id, request.leaderId());
    publishChanges(actorId, before, savedProject);

    return projectMapper.toResponse(savedProject);
  }

  /**
   * Hands the project back to nobody. Allowed because a project is permitted to exist without a
   * leader at all - the same state it can be created in - and forcing a replacement would mean a
   * departing leader has to be swapped for an arbitrary stand-in.
   */
  @Transactional
  public ProjectResponse removeLeader(Integer id, Integer actorId) {
    Project project = requireActiveProject(id);

    ProjectSnapshot before = ProjectSnapshot.of(project);

    project.setLeaderId(null);

    Project savedProject = projectRepository.save(project);

    evictLeaderCache(id, before.leaderId());
    publishChanges(actorId, before, savedProject);

    return projectMapper.toResponse(savedProject);
  }

  /**
   * Any status may follow any other - a project can be reopened out of {@code COMPLETED} or {@code
   * CANCELLED}. If that ever needs narrowing, this is the one place it has to happen.
   */
  @Transactional
  public ProjectResponse changeStatus(Integer id, Integer actorId, ChangeStatusRequest request) {
    Project project = requireActiveProject(id);

    ProjectSnapshot before = ProjectSnapshot.of(project);

    project.setStatus(request.status());

    Project savedProject = projectRepository.save(project);

    publishChanges(actorId, before, savedProject);

    return projectMapper.toResponse(savedProject);
  }

  /**
   * Soft-deletes the project and retires its member and team assignment rows, so nothing reachable
   * from a deleted project has to be filtered out again at read time.
   */
  @Transactional
  public void deleteProject(Integer id, Integer actorId) {
    Project project = requireActiveProject(id);

    project.setIsActive(false);

    projectRepository.save(project);

    // Two events for one moment, and neither can stand in for the other. The deactivation is
    // consumed inline, in this transaction, so the memberships are retired before anyone can read
    // them; the deletion is consumed after the commit so a fault in the audit path cannot fail the
    // delete. See ProjectDeletedEvent.
    eventPublisher.publishEvent(new ProjectDeactivatedEvent(id));
    eventPublisher.publishEvent(new ProjectDeletedEvent(id, actorId, OffsetDateTime.now()));
  }
}
