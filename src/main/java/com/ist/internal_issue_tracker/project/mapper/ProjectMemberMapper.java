package com.ist.internal_issue_tracker.project.mapper;

import com.ist.internal_issue_tracker.project.ProjectMember;
import com.ist.internal_issue_tracker.project.ProjectParticipant;
import com.ist.internal_issue_tracker.project.dto.ProjectMemberCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectMemberResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectParticipantResponse;
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

  public ProjectMemberResponse toResponse(ProjectMember projectMember) {
    return new ProjectMemberResponse(
        projectMember.getId(),
        projectMember.getUserId(),
        projectMember.getProjectId(),
        projectMember.getIsActive());
  }
}
