package com.ist.internal_issue_tracker.project.mapper;

import com.ist.internal_issue_tracker.project.ProjectMember;
import com.ist.internal_issue_tracker.project.ProjectParticipant;
import com.ist.internal_issue_tracker.project.ProjectStatus;
import com.ist.internal_issue_tracker.project.UserProject;
import com.ist.internal_issue_tracker.project.dto.ProjectMemberCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectMemberResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectParticipantResponse;
import com.ist.internal_issue_tracker.project.dto.UserProjectMembershipResponse;
import org.springframework.stereotype.Component;

@Component
public class ProjectMemberMapper {

  public ProjectMember toEntity(Integer projectId, ProjectMemberCreateRequest request) {
    ProjectMember projectMember = new ProjectMember();

    projectMember.setProjectId(projectId);
    projectMember.setUserId(request.userId());

    return projectMember;
  }

  public ProjectParticipantResponse toParticipantResponse(ProjectParticipant participant) {
    return new ProjectParticipantResponse(
        participant.getUserId(), Boolean.TRUE.equals(participant.getDirectlyAssigned()));
  }

  /** The status arrives as the raw column value, since a native projection cannot convert it. */
  public UserProjectMembershipResponse toUserProjectResponse(UserProject userProject) {
    return new UserProjectMembershipResponse(
        userProject.getProjectId(),
        userProject.getProjectName(),
        ProjectStatus.valueOf(userProject.getProjectStatus()),
        Boolean.TRUE.equals(userProject.getDirectlyAssigned()));
  }

  public ProjectMemberResponse toResponse(ProjectMember projectMember) {
    return new ProjectMemberResponse(
        projectMember.getId(),
        projectMember.getUserId(),
        projectMember.getProjectId(),
        projectMember.getIsActive(),
        projectMember.getUpdatedAt());
  }
}
