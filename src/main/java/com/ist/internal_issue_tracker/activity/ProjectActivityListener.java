package com.ist.internal_issue_tracker.activity;

import com.ist.internal_issue_tracker.shared.event.ProjectChangedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectCreatedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectDeletedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectField;
import com.ist.internal_issue_tracker.shared.event.ProjectFieldChange;
import com.ist.internal_issue_tracker.shared.event.ProjectMembershipEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Turns published project events into rows of {@code project_activities} - the project counterpart
 * of {@link IssueActivityListener}, and asynchronous for the same reasons.
 *
 * <p>It listens for {@code ProjectDeletedEvent} and not for {@code ProjectDeactivatedEvent}, which
 * is published in the same breath. The two are separate on purpose; see {@code ProjectDeletedEvent}.
 */
@Component
@RequiredArgsConstructor
class ProjectActivityListener {

  private final ProjectActivityRepository projectActivityRepository;

  @ApplicationModuleListener
  void on(ProjectCreatedEvent event) {
    record(
        event.projectId(), event.actorId(), ProjectActionType.CREATED, null, null, event.occurredAt());
  }

  @ApplicationModuleListener
  void on(ProjectChangedEvent event) {
    for (ProjectFieldChange change : event.changes()) {
      record(
          event.projectId(),
          event.actorId(),
          toActionType(change.field()),
          change.oldValue(),
          change.newValue(),
          event.occurredAt());
    }
  }

  @ApplicationModuleListener
  void on(ProjectDeletedEvent event) {
    record(
        event.projectId(), event.actorId(), ProjectActionType.DELETED, null, null, event.occurredAt());
  }

  /**
   * The subject goes into {@code newValue} when joining and {@code oldValue} when leaving, so the two
   * columns keep saying "what it was" and "what it is" - see {@link ProjectActivity}.
   */
  @ApplicationModuleListener
  void on(ProjectMembershipEvent event) {
    boolean added = event.change() == ProjectMembershipEvent.Change.ADDED;
    String subjectId = String.valueOf(event.subjectId());

    record(
        event.projectId(),
        event.actorId(),
        toActionType(event),
        added ? null : subjectId,
        added ? subjectId : null,
        event.occurredAt());
  }

  private static ProjectActionType toActionType(ProjectField field) {
    return switch (field) {
      case LEADER -> ProjectActionType.LEADER_UPDATED;
      case DETAILS -> ProjectActionType.DETAILS_UPDATED;
      case STATUS -> ProjectActionType.STATUS_UPDATED;
    };
  }

  private static ProjectActionType toActionType(ProjectMembershipEvent event) {
    return switch (event.subject()) {
      case USER ->
          event.change() == ProjectMembershipEvent.Change.ADDED
              ? ProjectActionType.USER_ADDED
              : ProjectActionType.USER_REMOVED;
      case TEAM ->
          event.change() == ProjectMembershipEvent.Change.ADDED
              ? ProjectActionType.TEAM_ADDED
              : ProjectActionType.TEAM_REMOVED;
    };
  }

  private void record(
      Integer projectId,
      Integer actorId,
      ProjectActionType actionType,
      String oldValue,
      String newValue,
      OffsetDateTime occurredAt) {

    if (projectActivityRepository.existsByProjectIdAndActionTypeAndCreatedAtAndOldValueAndNewValue(
        projectId, actionType, occurredAt, oldValue, newValue)) {
      return;
    }

    ProjectActivity activity = new ProjectActivity();
    activity.setProjectId(projectId);
    activity.setUserId(actorId);
    activity.setActionType(actionType);
    activity.setOldValue(oldValue);
    activity.setNewValue(newValue);
    activity.setCreatedAt(occurredAt);

    projectActivityRepository.save(activity);
  }
}
