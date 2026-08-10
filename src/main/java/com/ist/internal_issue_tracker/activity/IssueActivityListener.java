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
 * Turns issue events read off {@code issue-events} into rows of {@code issue_activities}. The only
 * writer this table has.
 *
 * <p>A broker consumer rather than the plain {@code @EventListener} the cleanup listeners use, and
 * the difference is deliberate. Those run inline in the publisher's transaction because a
 * deactivated user must never be readable on a roster for even an instant. Nothing reads an activity
 * row to make a decision, so there is no such window to close, and what comes with going through
 * Kafka is worth having: a fault in here cannot turn a successful issue write into a 500, the
 * backlog survives this process dying, and the day the activity module becomes its own service this
 * class moves with it and the publisher never learns.
 *
 * <p>It also means this runs after the commit, in another thread, reading a record that may have
 * been written days ago. Nothing here may read the clock or the database for anything the event does
 * not already carry - see {@code IssueActivity#createdAt}.
 *
 * <p>No transaction is opened around a handler. Each {@code save} commits on its own, so a
 * multi-field change lands one row at a time rather than all at once - which is fine here, because
 * delivery is at-least-once and every row is written behind the {@code existsBy…} check below: a
 * handler that fails half way is redelivered, skips what it already wrote, and finishes the rest.
 * The one case that does not converge is a failure that outlives its retries and ends in the dead
 * letter topic, which leaves the rows written before it in place. That is the price of not wrapping
 * this, and it is worth paying: for an audit log, part of an operation recorded is better than none
 * of it, and the dead letter says what was lost either way.
 *
 * <p>Class-level {@code @KafkaListener} with {@code @KafkaHandler} methods rather than three
 * separate listeners, because all three types share one topic and one group: the handler is chosen
 * from the {@code __TypeId__} header the producer writes - see {@code KafkaMessagingConfig}.
 */
@Component
@KafkaListener(topics = "issue-events", groupId = "activity-issue-writer")
@RequiredArgsConstructor
class IssueActivityListener {

  private static final Logger log = LoggerFactory.getLogger(IssueActivityListener.class);

  private final IssueActivityRepository issueActivityRepository;

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

  /**
   * Swallows anything on the topic this class was not written for, and complains about it.
   *
   * <p>It has to swallow: an unrecognised type raised as an error is an error no retry can fix, and
   * it would stop the partition and everything queued behind it.
   *
   * <p>It has to complain, at {@code WARN} and not lower, because on this topic there is no such
   * thing as a payload we are not interested in - every type published to {@code issue-events} has a
   * handler above. Reaching here means an event was accepted from a publisher and dropped: a
   * producer sending something new, a type header that did not survive, a converter not configured
   * the way it is here. Logged quietly, that is an activity log missing rows and nothing anywhere
   * saying so.
   */
  @KafkaHandler(isDefault = true)
  void unknown(Object payload) {
    log.warn(
        "Dropping unhandled payload on issue-events: {}. Nothing was written to issue_activities.",
        payload == null ? "null" : payload.getClass().getName());
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
