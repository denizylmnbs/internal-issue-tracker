package com.ist.internal_issue_tracker.project.mapper;

import com.ist.internal_issue_tracker.project.ProjectTeam;
import com.ist.internal_issue_tracker.project.dto.ProjectTeamCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectTeamResponse;
import org.springframework.stereotype.Component;

@Component
public class ProjectTeamMapper {

  public ProjectTeam toEntity(Integer projectId, ProjectTeamCreateRequest request) {
    ProjectTeam projectTeam = new ProjectTeam();

    projectTeam.setProjectId(projectId);
    projectTeam.setTeamId(request.teamId());

    return projectTeam;
  }

  public ProjectTeamResponse toResponse(ProjectTeam projectTeam) {
    return new ProjectTeamResponse(
        projectTeam.getId(),
        projectTeam.getTeamId(),
        projectTeam.getProjectId(),
        projectTeam.getIsActive());
  }
}
