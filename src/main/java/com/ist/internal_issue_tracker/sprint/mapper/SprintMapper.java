package com.ist.internal_issue_tracker.sprint.mapper;

import com.ist.internal_issue_tracker.sprint.Sprint;
import com.ist.internal_issue_tracker.sprint.dto.SprintCreateRequest;
import com.ist.internal_issue_tracker.sprint.dto.SprintResponse;
import com.ist.internal_issue_tracker.sprint.dto.SprintUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class SprintMapper {

  /**
   * The project comes from the path, not the body. {@code defaultStatus} is this project's {@code
   * SPRINT_STATUS} default code, resolved by {@code SprintService} - the entity no longer has a
   * hardcoded default now that the vocabulary is per-project data.
   */
  public Sprint toEntity(Integer projectId, SprintCreateRequest request, String defaultStatus) {
    Sprint sprint = new Sprint();

    sprint.setProjectId(projectId);
    sprint.setName(request.name());
    sprint.setDescription(request.description());
    sprint.setStartDate(request.startDate());
    sprint.setEndDate(request.endDate());
    sprint.setStatus(defaultStatus);

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
        sprint.getCommittedPoints(),
        sprint.getCommittedAt(),
        sprint.getCreatedAt(),
        sprint.getUpdatedAt());
  }
}
