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

  /**
   * {@code updatedAt} is nullable (no default, unlike {@code createdAt}) for any row that has never
   * gone through Hibernate's {@code @UpdateTimestamp} path. Falling back to {@code createdAt} there
   * is not a guess: for an assignment never touched since it was created, "assigned" and "created"
   * are the same moment.
   */
  public ProjectTeamResponse toResponse(ProjectTeam projectTeam) {
    return new ProjectTeamResponse(
        projectTeam.getId(),
        projectTeam.getTeamId(),
        projectTeam.getProjectId(),
        projectTeam.getIsActive(),
        projectTeam.getUpdatedAt() != null
            ? projectTeam.getUpdatedAt()
            : projectTeam.getCreatedAt());
  }
}
