package com.ist.internal_issue_tracker.activity;

import com.ist.internal_issue_tracker.shared.event.IssueChangedEvent;
import com.ist.internal_issue_tracker.shared.event.IssueCreatedEvent;
import com.ist.internal_issue_tracker.shared.event.IssueDeletedEvent;
import com.ist.internal_issue_tracker.shared.event.IssueDimensions;
import com.ist.internal_issue_tracker.shared.event.IssueField;
import com.ist.internal_issue_tracker.shared.event.IssueFieldChange;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Turns published issue events into rows of {@code issue_activities}. The only writer this table
 * has.
 *
 * <p>{@code @ApplicationModuleListener} rather than the plain {@code @EventListener} the cleanup
 * listeners use, and the difference is deliberate. Those run inline in the publisher's transaction
 * because a deactivated user must never be readable on a roster for even an instant. Nothing reads
 * an activity row to make a decision, so there is no such window to close, and the two properties
 * that come with the asynchronous form are worth having: a fault in here cannot turn a successful
 * issue write into a 500, and the publication registry replays whatever was in flight when the
 * process died.
 *
 * <p>It also means this listener runs after the commit, on another thread. Nothing here may read the
 * clock or the database for anything the event does not already carry - see {@code
 * IssueActivity#createdAt}.
 */
@Component
@RequiredArgsConstructor
class IssueActivityListener {

  private final IssueActivityRepository issueActivityRepository;

  @ApplicationModuleListener
  void on(IssueCreatedEvent event) {
    record(
        event.issueId(),
        event.projectId(),
        event.actorId(),
        IssueActionType.CREATED,
        null,
        null,
        event.occurredAt(),
        event.dimensions());
  }

  /**
   * One row per changed field, all of them stamped with the event's single {@code occurredAt} -
   * they describe one operation, not several - and all of them carrying the same dimensions, which
   * are the issue's state after the whole operation rather than after each field of it.
   */
  @ApplicationModuleListener
  void on(IssueChangedEvent event) {
    for (IssueFieldChange change : event.changes()) {
      record(
          event.issueId(),
          event.projectId(),
          event.actorId(),
          toActionType(change.field()),
          change.oldValue(),
          change.newValue(),
          event.occurredAt(),
          event.dimensions());
    }
  }

  @ApplicationModuleListener
  void on(IssueDeletedEvent event) {
    record(
        event.issueId(),
        event.projectId(),
        event.actorId(),
        IssueActionType.DELETED,
        null,
        null,
        event.occurredAt(),
        event.dimensions());
  }

  /** The one place the publisher's vocabulary meets the table's - see {@link IssueActionType}. */
  private static IssueActionType toActionType(IssueField field) {
    return switch (field) {
      case STATUS -> IssueActionType.STATUS_UPDATED;
      case PRIORITY -> IssueActionType.PRIORITY_UPDATED;
      case ASSIGNEE_USER -> IssueActionType.ASSIGNEE_USER_UPDATED;
      case ASSIGNEE_TEAM -> IssueActionType.ASSIGNEE_TEAM_UPDATED;
      case SPRINT -> IssueActionType.SPRINT_UPDATED;
      case STORY_POINT -> IssueActionType.STORY_POINT_UPDATED;
      case DETAILS -> IssueActionType.DETAILS_UPDATED;
    };
  }

  private void record(
      Integer issueId,
      Integer projectId,
      Integer actorId,
      IssueActionType actionType,
      String oldValue,
      String newValue,
      OffsetDateTime occurredAt,
      IssueDimensions dimensions) {

    // see IssueActivityRepository#existsByIssueIdAndActionTypeAndCreatedAt for why this is a check
    // and not a constraint
    if (issueActivityRepository.existsByIssueIdAndActionTypeAndCreatedAt(
        issueId, actionType, occurredAt)) {
      return;
    }

    IssueActivity activity = new IssueActivity();
    activity.setIssueId(issueId);
    activity.setProjectId(projectId);
    activity.setUserId(actorId);
    activity.setActionType(actionType);
    activity.setOldValue(oldValue);
    activity.setNewValue(newValue);
    activity.setCreatedAt(occurredAt);

    if (dimensions != null) {
      activity.setIssueType(dimensions.type());
      activity.setPriority(dimensions.priority());
      activity.setStoryPoint(dimensions.storyPoint());
      activity.setSprintId(dimensions.sprintId());
    }

    issueActivityRepository.save(activity);
  }
}
