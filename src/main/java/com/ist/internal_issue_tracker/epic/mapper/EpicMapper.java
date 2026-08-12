package com.ist.internal_issue_tracker.epic.mapper;

import com.ist.internal_issue_tracker.epic.Epic;
import com.ist.internal_issue_tracker.epic.dto.EpicCreateRequest;
import com.ist.internal_issue_tracker.epic.dto.EpicResponse;
import com.ist.internal_issue_tracker.epic.dto.EpicUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class EpicMapper {

  /**
   * The project comes from the path and the reporter from the authenticated caller, so both are
   * parameters rather than fields of the request. {@code defaultStatus} is this project's {@code
   * EPIC_STATUS} default code, resolved by {@code EpicService} - the entity no longer has a
   * hardcoded default now that the vocabulary is per-project data.
   */
  public Epic toEntity(
      Integer projectId, Integer reporterId, EpicCreateRequest request, String defaultStatus) {
    Epic epic = new Epic();

    epic.setProjectId(projectId);
    epic.setReporterId(reporterId);
    epic.setName(request.name());
    epic.setDescription(request.description());
    epic.setStatus(defaultStatus);

    return epic;
  }

  /** Status, project and reporter are untouched here - each has its own path or none at all. */
  public void updateEntity(Epic epic, EpicUpdateRequest request) {
    epic.setName(request.name());
    epic.setDescription(request.description());
  }

  public EpicResponse toResponse(Epic epic) {
    return new EpicResponse(
        epic.getId(),
        epic.getProjectId(),
        epic.getName(),
        epic.getDescription(),
        epic.getStatus(),
        epic.getReporterId(),
        epic.getCreatedAt(),
        epic.getUpdatedAt());
  }
}
