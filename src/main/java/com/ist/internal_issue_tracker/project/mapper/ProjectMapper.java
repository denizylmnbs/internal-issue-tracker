package com.ist.internal_issue_tracker.project.mapper;

import com.ist.internal_issue_tracker.project.Project;
import com.ist.internal_issue_tracker.project.dto.ProjectCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectDetailResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

  /** The status is left alone so the entity's own {@code PLANNING} default stands. */
  public Project toEntity(ProjectCreateRequest request) {
    Project project = new Project();

    project.setName(request.name());
    project.setDescription(request.description());
    project.setStartDate(request.startDate());
    project.setEndDate(request.endDate());
    project.setLeaderId(request.leaderId());

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
