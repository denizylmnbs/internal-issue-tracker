package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.project.dto.ProjectMemberCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectMemberResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectParticipantResponse;
import com.ist.internal_issue_tracker.project.dto.UserProjectMembershipResponse;
import com.ist.internal_issue_tracker.project.exception.ProjectMemberErrorCode;
import com.ist.internal_issue_tracker.project.exception.ProjectNotFoundException;
import com.ist.internal_issue_tracker.project.mapper.ProjectMemberMapper;
import com.ist.internal_issue_tracker.shared.event.ProjectMembershipEvent;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.security.Role;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.util.Set;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Users assigned to a project directly. Membership through an assigned team is {@link
 * ProjectTeamService}'s business, and the two together are what "works on this project" means.
 *
 * <p><b>Two user ids, and they are not the same person.</b> The one on the request - or the path, on
 * a removal - is the <em>subject</em>, whose membership is being added or retired. {@code actorId} is
 * whoever is performing that, and is the one bound for {@code project_activities.user_id}: a lead
 * taking someone off a project is a fact about the lead, not about the person removed. See {@code
 * IssueService} for the full reasoning.
 */
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

  /**
   * Stands in for an empty team list, because {@code IN ()} is a syntax error rather than an empty
   * result. No team can carry it, so the team half of the union matches nothing - which is exactly
   * what a user on no team should see.
   */
  private static final Set<Integer> NO_TEAMS = Set.of(-1);

  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectMemberMapper projectMemberMapper;
  private final ProjectRepository projectRepository;
  private final UserLookup userLookup;
  private final TeamLookup teamLookup;
  private final ApplicationEventPublisher eventPublisher;

  private void requireActiveProject(Integer projectId) {
    if (!projectRepository.existsByIdAndIsActiveTrue(projectId)) {
      throw new ProjectNotFoundException(projectId);
    }
  }

  /**
   * Revives an assignment that was soft-deleted, or rejects one that is still live. Removing a
   * member only clears {@code isActive}, so the row outlives them and would collide with a fresh
   * insert on {@code unique_active_project_user} the moment they were added back.
   */
  private static ProjectMember requireInactive(ProjectMember membership) {
    if (Boolean.TRUE.equals(membership.getIsActive())) {
      throw new AppException(ProjectMemberErrorCode.PROJECT_MEMBER_ALREADY_EXIST);
    }

    membership.setIsActive(true);
    return membership;
  }

  /**
   * Adding someone who was taken off the project earlier reactivates their original row rather than
   * opening a second one, so a project's history stays one row per person - see {@code
   * TeamMemberService#createTeamMember}.
   */
  @Transactional
  public ProjectMemberResponse createProjectMember(
      Integer projectId, Integer actorId, ProjectMemberCreateRequest request) {
    // variables
    Integer userId = request.userId();
    Role role = Role.DEVELOPER; // minimum role to be assigned to a project

    // a soft-deleted project takes no new members
    requireActiveProject(projectId);

    // check user is valid
    if (!userLookup.existsActiveUser(userId)) {
      throw new AppException(ProjectMemberErrorCode.USER_NOT_FOUND);
    }

    // check user role
    if (!userLookup.hasAtLeastRole(userId, role)) {
      throw new AppException(ProjectMemberErrorCode.USER_ROLE_NOT_ENOUGH);
    }

    ProjectMember projectMember =
        projectMemberRepository
            .findFirstByProjectIdAndUserIdOrderByIdDesc(projectId, userId)
            .map(ProjectMemberService::requireInactive)
            .orElseGet(() -> projectMemberMapper.toEntity(projectId, request));

    ProjectMember savedProjectMember;
    try {
      savedProjectMember = projectMemberRepository.save(projectMember);
    } catch (DataIntegrityViolationException e) {
      // unique_active_project_user: another request assigned the same user first
      throw new AppException(ProjectMemberErrorCode.PROJECT_MEMBER_ALREADY_EXIST);
    }

    // userId is the subject being added; actorId is whoever is adding them
    eventPublisher.publishEvent(
        new ProjectMembershipEvent(
            projectId,
            userId,
            ProjectMembershipEvent.Subject.USER,
            ProjectMembershipEvent.Change.ADDED,
            actorId,
            OffsetDateTime.now()));

    return projectMemberMapper.toResponse(savedProjectMember);
  }

  /**
   * Only the directly assigned users, which is what this endpoint's POST and DELETE operate on.
   * Members reached through a team are not listed here.
   */
  public PagedResponse<ProjectMemberResponse> getProjectMembers(
      Integer projectId, Pageable pageable) {
    requireActiveProject(projectId);

    Page<ProjectMember> projectMembers =
        projectMemberRepository.findAllByProjectIdAndIsActiveTrue(projectId, pageable);
    Page<ProjectMemberResponse> responsePage = projectMembers.map(projectMemberMapper::toResponse);

    return PagedResponse.from(responsePage);
  }

  /**
   * Everyone who works on the project, by either route - the population {@code memberCount} reports.
   *
   * <p>The caller's sort is deliberately dropped. The query groups a union, so there is nothing to
   * order by but the user id, and an unordered paged query would hand back overlapping pages.
   */
  public PagedResponse<ProjectParticipantResponse> getProjectParticipants(
      Integer projectId, Pageable pageable) {
    requireActiveProject(projectId);

    Pageable byUserId =
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("user_id"));

    Page<ProjectParticipant> participants =
        projectMemberRepository.findActiveParticipants(projectId, byUserId);

    return PagedResponse.from(participants.map(projectMemberMapper::toParticipantResponse));
  }

  /**
   * The projects a user works on, by either route. Sorting is fixed for the same reason as {@link
   * #getProjectParticipants}.
   *
   * <p>The team route is resolved in two steps - team ids from {@code team}, then the projects those
   * teams are on - so that the query below reads none of {@code team}'s tables.
   */
  public PagedResponse<UserProjectMembershipResponse> getProjectsByUserId(
      Integer userId, Pageable pageable) {
    // 404, not the 422 that PROJECT_MEMBER's USER_NOT_FOUND carries - see
    // TeamMemberService#getTeamsByUserId for why the two cases differ
    if (!userLookup.existsActiveUser(userId)) {
      throw ResourceNotFoundException.of("User", userId);
    }

    Pageable byProjectId =
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("project_id"));

    Set<Integer> teamIds = teamLookup.activeTeamIdsOfUser(userId);

    Page<UserProject> projects =
        projectMemberRepository.findActiveProjectsByUserId(
            userId, teamIds.isEmpty() ? NO_TEAMS : teamIds, byProjectId);

    return PagedResponse.from(projects.map(projectMemberMapper::toUserProjectResponse));
  }

  /** Soft delete - see {@code TeamMemberService#removeTeamMember} for why the entity is loaded. */
  @Transactional
  public void removeProjectMember(Integer projectId, Integer userId, Integer actorId) {
    ProjectMember membership =
        projectMemberRepository
            .findByProjectIdAndUserIdAndIsActiveTrue(projectId, userId)
            .orElseThrow(() -> new AppException(ProjectMemberErrorCode.PROJECT_MEMBER_NOT_FOUND));

    membership.setIsActive(false);
    projectMemberRepository.save(membership);

    eventPublisher.publishEvent(
        new ProjectMembershipEvent(
            projectId,
            userId,
            ProjectMembershipEvent.Subject.USER,
            ProjectMembershipEvent.Change.REMOVED,
            actorId,
            OffsetDateTime.now()));
  }
}
