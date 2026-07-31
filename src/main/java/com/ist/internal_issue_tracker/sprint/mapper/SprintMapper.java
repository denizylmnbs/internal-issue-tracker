package com.ist.internal_issue_tracker.sprint.mapper;

import com.ist.internal_issue_tracker.sprint.Sprint;
import com.ist.internal_issue_tracker.sprint.dto.SprintCreateRequest;
import com.ist.internal_issue_tracker.sprint.dto.SprintResponse;
import com.ist.internal_issue_tracker.sprint.dto.SprintUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class SprintMapper {

  /**
   * The project comes from the path, not the body. The status is left alone so the entity's own
   * {@code TODO} default stands.
   */
  public Sprint toEntity(Integer projectId, SprintCreateRequest request) {
    Sprint sprint = new Sprint();

    sprint.setProjectId(projectId);
    sprint.setName(request.name());
    sprint.setDescription(request.description());
    sprint.setStartDate(request.startDate());
    sprint.setEndDate(request.endDate());

    return sprint;
  }

  /** Status and project are untouched here - each has its own path or none at all. */
  public void updateEntity(Sprint sprint, SprintUpdateRequest request) {
    sprint.setName(request.name());
    sprint.setDescription(request.description());
    sprint.setStartDate(request.startDate());
    sprint.setEndDate(request.endDate());
  }

  public SprintResponse toResponse(Sprint sprint) {
    return new SprintResponse(
        sprint.getId(),
        sprint.getProjectId(),
        sprint.getName(),
        sprint.getDescription(),
        sprint.getStartDate(),
        sprint.getEndDate(),
        sprint.getStatus(),
        sprint.getCreatedAt(),
        sprint.getUpdatedAt());
  }
}
