package com.ist.internal_issue_tracker.project.mapper;

import com.ist.internal_issue_tracker.project.Project;
import com.ist.internal_issue_tracker.project.dto.ProjectCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectDetailResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

  /**
   * {@code defaultStatus} is the global {@code PROJECT_STATUS} default code, resolved by {@code
   * ProjectService} - the entity no longer has a hardcoded {@code PLANNING} default now that the
   * vocabulary is field-definition data.
   */
  public Project toEntity(ProjectCreateRequest request, String defaultStatus) {
    Project project = new Project();

    project.setName(request.name());
    project.setDescription(request.description());
    project.setStartDate(request.startDate());
    project.setEndDate(request.endDate());
    project.setLeaderId(request.leaderId());
    project.setStatus(defaultStatus);

    return project;
  }

  public void updateEntity(Project project, ProjectUpdateRequest request) {
    project.setName(request.name());
    project.setDescription(request.description());
    project.setStartDate(request.startDate());
    project.setEndDate(request.endDate());
  }

  public ProjectDetailResponse toDetailResponse(Project project, long memberCount, long teamCount) {
    return new ProjectDetailResponse(
        project.getId(),
        project.getName(),
        project.getDescription(),
        project.getStartDate(),
        project.getEndDate(),
        project.getLeaderId(),
        project.getStatus(),
        project.getIsActive(),
        memberCount,
        teamCount,
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  public ProjectResponse toResponse(Project project) {
    return new ProjectResponse(
        project.getId(),
        project.getName(),
        project.getDescription(),
        project.getStartDate(),
        project.getEndDate(),
        project.getLeaderId(),
        project.getStatus(),
        project.getIsActive(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }
}
