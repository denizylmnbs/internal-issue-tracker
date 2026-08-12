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

/**
 * Turns published sprint events into rows of {@code sprint_activities} - the sprint counterpart of
 * {@link IssueActivityListener}, and asynchronous for the same reasons.
 */
@Component
@KafkaListener(topics = "sprint-events", groupId = "activity-sprint-writer")
@RequiredArgsConstructor
class SprintActivityListener {

  private static final Logger log = LoggerFactory.getLogger(SprintActivityListener.class);

  private final SprintActivityRepository sprintActivityRepository;

  private static SprintActionType toActionType(SprintField field) {
    return switch (field) {
      case STATUS -> SprintActionType.STATUS_UPDATED;
      case DATES -> SprintActionType.DATES_UPDATED;
      case DETAILS -> SprintActionType.DETAILS_UPDATED;
    };
  }

  @KafkaHandler
  void on(SprintCreatedEvent event) {
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
  void on(SprintChangedEvent event) {
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
  void on(SprintDeletedEvent event) {
    record(
        event.sprintId(),
        event.projectId(),
        event.actorId(),
        SprintActionType.DELETED,
        null,
        null,
        event.occurredAt());
  }

  // Anything on sprint-events this class has no handler for. Swallowed rather than thrown: an
  // unrecognised type is an error no retry can fix, so it would stop the partition and
  // everything behind it. Warned about, because every type published here does have a
  // handler above - reaching this means an activity row was dropped.
  @KafkaHandler(isDefault = true)
  void unknown(Object payload) {
    log.warn(
        "Dropped unhandled payload on sprint-events: {}. Nothing written to sprint_activities.",
        payload == null ? "null" : payload.getClass().getName());
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
