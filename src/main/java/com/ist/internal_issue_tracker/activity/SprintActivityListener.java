package com.ist.internal_issue_tracker.activity;

import com.ist.internal_issue_tracker.shared.event.SprintChangedEvent;
import com.ist.internal_issue_tracker.shared.event.SprintCreatedEvent;
import com.ist.internal_issue_tracker.shared.event.SprintDeletedEvent;
import com.ist.internal_issue_tracker.shared.event.SprintField;
import com.ist.internal_issue_tracker.shared.event.SprintFieldChange;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Turns published sprint events into rows of {@code sprint_activities} - the sprint counterpart of
 * {@link IssueActivityListener}, and asynchronous for the same reasons.
 */
@Component
@RequiredArgsConstructor
class SprintActivityListener {

  private final SprintActivityRepository sprintActivityRepository;

  @ApplicationModuleListener
  void on(SprintCreatedEvent event) {
    record(event.sprintId(), event.actorId(), SprintActionType.CREATED, null, null, event.occurredAt());
  }

  @ApplicationModuleListener
  void on(SprintChangedEvent event) {
    for (SprintFieldChange change : event.changes()) {
      record(
          event.sprintId(),
          event.actorId(),
          toActionType(change.field()),
          change.oldValue(),
          change.newValue(),
          event.occurredAt());
    }
  }

  @ApplicationModuleListener
  void on(SprintDeletedEvent event) {
    record(event.sprintId(), event.actorId(), SprintActionType.DELETED, null, null, event.occurredAt());
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
    activity.setUserId(actorId);
    activity.setActionType(actionType);
    activity.setOldValue(oldValue);
    activity.setNewValue(newValue);
    activity.setCreatedAt(occurredAt);

    sprintActivityRepository.save(activity);
  }
}
