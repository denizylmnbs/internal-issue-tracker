package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.project.dto.ProjectMemberCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectMemberResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectParticipantResponse;
import com.ist.internal_issue_tracker.project.dto.UserProjectMembershipResponse;
import com.ist.internal_issue_tracker.project.exception.ProjectMemberErrorCode;
import com.ist.internal_issue_tracker.project.exception.ProjectNotFoundException;
import com.ist.internal_issue_tracker.project.mapper.ProjectMemberMapper;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.security.Role;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Users assigned to a project directly. Membership through an assigned team is {@link
 * ProjectTeamService}'s business, and the two together are what "works on this project" means.
 */
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectMemberMapper projectMemberMapper;
  private final ProjectRepository projectRepository;
  private final UserLookup userLookup;

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
  public ProjectMemberResponse createProjectMember(
      Integer projectId, ProjectMemberCreateRequest request) {
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

    return projectMemberMapper.toResponse(savedProjectMember);
  }

  /**
   * Only the directly assigned users, which is what this endpoint's POST and DELETE operate on.
   * Members reached through a team are not listed here.
   */
  public PagedResponse<ProjectMemberResponse> getProjectMembers(
      Integer projectId, Pageable pageable) {
    requireActiveProject(projectId);

    Pageable byId =
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));

    Page<ProjectMember> projectMembers =
        projectMemberRepository.findActiveMembersOfProject(projectId, byId);
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

    Page<UserProject> projects =
        projectMemberRepository.findActiveProjectsByUserId(userId, byProjectId);

    return PagedResponse.from(projects.map(projectMemberMapper::toUserProjectResponse));
  }

  /** Soft delete - see {@code TeamMemberService#removeTeamMember} for why the entity is loaded. */
  public void removeProjectMember(Integer projectId, Integer userId) {
    ProjectMember membership =
        projectMemberRepository
            .findByProjectIdAndUserIdAndIsActiveTrue(projectId, userId)
            .orElseThrow(() -> new AppException(ProjectMemberErrorCode.PROJECT_MEMBER_NOT_FOUND));

    membership.setIsActive(false);
    projectMemberRepository.save(membership);
  }
}
