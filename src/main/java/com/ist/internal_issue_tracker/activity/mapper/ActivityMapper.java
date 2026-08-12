package com.ist.internal_issue_tracker.activity.mapper;

import com.ist.internal_issue_tracker.activity.ActivityFeedRow;
import com.ist.internal_issue_tracker.activity.ActivityScope;
import com.ist.internal_issue_tracker.activity.IssueActivity;
import com.ist.internal_issue_tracker.activity.ProjectActivity;
import com.ist.internal_issue_tracker.activity.SprintActivity;
import com.ist.internal_issue_tracker.activity.dto.ActivityResponse;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * Only {@code toResponse}, in four overloads - there is no {@code toEntity} because nothing outside
 * the listeners may create a history row, and no {@code updateEntity} because history is
 * append-only.
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
        activity.getCreatedAt(),
        ActivityScope.ISSUE.name(),
        activity.getIssueId());
  }

  public ActivityResponse toResponse(SprintActivity activity) {
    return new ActivityResponse(
        activity.getId(),
        activity.getUserId(),
        activity.getActionType().name(),
        activity.getOldValue(),
        activity.getNewValue(),
        activity.getCreatedAt(),
        ActivityScope.SPRINT.name(),
        activity.getSprintId());
  }

  public ActivityResponse toResponse(ProjectActivity activity) {
    return new ActivityResponse(
        activity.getId(),
        activity.getUserId(),
        activity.getActionType().name(),
        activity.getOldValue(),
        activity.getNewValue(),
        activity.getCreatedAt(),
        ActivityScope.PROJECT.name(),
        activity.getProjectId());
  }

  /**
   * The union query already tags each row with its scope and subject - nothing to convert there.
   * {@code createdAt} does need converting: the connection's session time zone is pinned to UTC
   * (see {@code application.properties}), so the {@code Instant} the native query hands back and an
   * {@code OffsetDateTime} at {@link ZoneOffset#UTC} name the same point in time.
   */
  public ActivityResponse toResponse(ActivityFeedRow row) {
    return new ActivityResponse(
        row.getId(),
        row.getUserId(),
        row.getActionType(),
        row.getOldValue(),
        row.getNewValue(),
        row.getCreatedAt().atOffset(ZoneOffset.UTC),
        row.getScope(),
        row.getSubjectId());
  }
}
