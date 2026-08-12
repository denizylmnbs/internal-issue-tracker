package com.ist.internal_issue_tracker.activity;

import com.ist.internal_issue_tracker.shared.event.IssueChangedEvent;
import com.ist.internal_issue_tracker.shared.event.IssueCreatedEvent;
import com.ist.internal_issue_tracker.shared.event.IssueDeletedEvent;
import com.ist.internal_issue_tracker.shared.event.IssueDimensions;
import com.ist.internal_issue_tracker.shared.event.IssueField;
import com.ist.internal_issue_tracker.shared.event.IssueFieldChange;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Turns published issue events into rows of {@code issue_activities}. The only writer this table
 * has.
 *
 * <p>A Kafka consumer rather than the plain {@code @EventListener} the cleanup listeners use, and
 * the difference is deliberate. Those run inline in the publisher's transaction because a
 * deactivated user must never be readable on a roster for even an instant. Nothing reads an
 * activity row to make a decision, so there is no such window to close, and the two properties that
 * come with the asynchronous form are worth having: a fault in here cannot turn a successful issue
 * write into a 500, and the topic still holds the event if this process dies mid-way.
 *
 * <p>It also means this listener runs after the commit, on another thread. Nothing here may read
 * the clock or the database for anything the event does not already carry - see {@code
 * IssueActivity#createdAt}.
 */
@Component
@KafkaListener(topics = "issue-events", groupId = "activity-issue-writer")
@RequiredArgsConstructor
class IssueActivityListener {

  private static final Logger log = LoggerFactory.getLogger(IssueActivityListener.class);

  private final IssueActivityRepository issueActivityRepository;

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

  @KafkaHandler
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
  @KafkaHandler
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

  @KafkaHandler
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
  // Anything on issue-events this class has no handler for. Swallowed rather than thrown: an
  // unrecognised type is an error no retry can fix, so it would stop the partition and
  // everything behind it. Warned about, because every type published here does have a
  // handler above - reaching this means an activity row was dropped.
  @KafkaHandler(isDefault = true)
  void unknown(Object payload) {
    log.warn(
        "Dropped unhandled payload on issue-events: {}. Nothing written to issue_activities.",
        payload == null ? "null" : payload.getClass().getName());
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
