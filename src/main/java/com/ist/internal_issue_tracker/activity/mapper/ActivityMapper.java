package com.ist.internal_issue_tracker.activity.mapper;

import com.ist.internal_issue_tracker.activity.IssueActivity;
import com.ist.internal_issue_tracker.activity.ProjectActivity;
import com.ist.internal_issue_tracker.activity.SprintActivity;
import com.ist.internal_issue_tracker.activity.dto.ActivityResponse;
import org.springframework.stereotype.Component;

/**
 * Only {@code toResponse}, in three overloads - there is no {@code toEntity} because nothing outside
 * the listeners may create a history row, and no {@code updateEntity} because history is append-only.
 */
@Component
public class ActivityMapper {

  public ActivityResponse toResponse(IssueActivity activity) {
    return new ActivityResponse(
        activity.getId(),
        activity.getUserId(),
        activity.getActionType().name(),
        activity.getOldValue(),
        activity.getNewValue(),
        activity.getCreatedAt());
  }

  public ActivityResponse toResponse(SprintActivity activity) {
    return new ActivityResponse(
        activity.getId(),
        activity.getUserId(),
        activity.getActionType().name(),
        activity.getOldValue(),
        activity.getNewValue(),
        activity.getCreatedAt());
  }

  public ActivityResponse toResponse(ProjectActivity activity) {
    return new ActivityResponse(
        activity.getId(),
        activity.getUserId(),
        activity.getActionType().name(),
        activity.getOldValue(),
        activity.getNewValue(),
        activity.getCreatedAt());
  }
}
