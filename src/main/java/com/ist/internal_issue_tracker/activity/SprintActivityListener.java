package com.ist.internal_issue_tracker.activity;

import com.ist.internal_issue_tracker.shared.event.SprintChangedEvent;
import com.ist.internal_issue_tracker.shared.event.SprintCreatedEvent;
import com.ist.internal_issue_tracker.shared.event.SprintDeletedEvent;
import com.ist.internal_issue_tracker.shared.event.SprintField;
import com.ist.internal_issue_tracker.shared.event.SprintFieldChange;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns sprint events read off {@code sprint-events} into rows of {@code sprint_activities} - the
 * sprint counterpart of {@link IssueActivityListener}, and a broker consumer for the same reasons.
 * That class also explains why the handlers are public and why they are {@code @KafkaHandler}s on a
 * class-level listener rather than three of their own.
 *
 * <p>Its own group, so a sprint event that cannot be written leaves the issue feed alone.
 */
@Component
@KafkaListener(topics = "sprint-events", groupId = "activity-sprint-writer")
@RequiredArgsConstructor
class SprintActivityListener {

  private static final Logger log = LoggerFactory.getLogger(SprintActivityListener.class);

  private final SprintActivityRepository sprintActivityRepository;

  @KafkaHandler
  @Transactional
  public void on(SprintCreatedEvent event) {
    record(
        event.sprintId(),
        event.projectId(),
        event.actorId(),
        SprintActionType.CREATED,
        null,
        null,
        event.occurredAt());
  }

  @KafkaHandler
  @Transactional
  public void on(SprintChangedEvent event) {
    for (SprintFieldChange change : event.changes()) {
      record(
          event.sprintId(),
          event.projectId(),
          event.actorId(),
          toActionType(change.field()),
          change.oldValue(),
          change.newValue(),
          event.occurredAt());
    }
  }

  @KafkaHandler
  @Transactional
  public void on(SprintDeletedEvent event) {
    record(
        event.sprintId(),
        event.projectId(),
        event.actorId(),
        SprintActionType.DELETED,
        null,
        null,
        event.occurredAt());
  }

  /** See {@code IssueActivityListener#unknown}, including why this is a warning. */
  @KafkaHandler(isDefault = true)
  public void unknown(Object payload) {
    log.warn(
        "Dropping unhandled payload on sprint-events: {}. Nothing was written to sprint_activities.",
        payload == null ? "null" : payload.getClass().getName());
  }

  private static SprintActionType toActionType(SprintField field) {
    return switch (field) {
      case STATUS -> SprintActionType.STATUS_UPDATED;
      case DATES -> SprintActionType.DATES_UPDATED;
      case DETAILS -> SprintActionType.DETAILS_UPDATED;
    };
  }

  private void record(
      Integer sprintId,
      Integer projectId,
      Integer actorId,
      SprintActionType actionType,
      String oldValue,
      String newValue,
      OffsetDateTime occurredAt) {

    if (sprintActivityRepository.existsBySprintIdAndActionTypeAndCreatedAt(
        sprintId, actionType, occurredAt)) {
      return;
    }

    SprintActivity activity = new SprintActivity();
    activity.setSprintId(sprintId);
    activity.setProjectId(projectId);
    activity.setUserId(actorId);
    activity.setActionType(actionType);
    activity.setOldValue(oldValue);
    activity.setNewValue(newValue);
    activity.setCreatedAt(occurredAt);

    sprintActivityRepository.save(activity);
  }
}
