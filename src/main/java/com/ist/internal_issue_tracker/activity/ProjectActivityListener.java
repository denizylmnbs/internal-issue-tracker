package com.ist.internal_issue_tracker.activity;

import com.ist.internal_issue_tracker.shared.event.ProjectChangedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectCreatedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectDeletedEvent;
import com.ist.internal_issue_tracker.shared.event.ProjectField;
import com.ist.internal_issue_tracker.shared.event.ProjectFieldChange;
import com.ist.internal_issue_tracker.shared.event.ProjectMembershipEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Turns project events read off {@code project-events} into rows of {@code project_activities} - the
 * project counterpart of {@link IssueActivityListener}, and a broker consumer for the same reasons.
 * That class also explains why the handlers run without a transaction and why they are
 * {@code @KafkaHandler}s on a class-level listener rather than four of their own.
 *
 * <p>It handles {@code ProjectDeletedEvent} and not {@code ProjectDeactivatedEvent}, which is
 * published in the same breath. The two are separate on purpose, and now for a second reason: only
 * the deleted half is on a topic at all. See {@code ProjectDeletedEvent}.
 *
 * <p>{@code ProjectMembershipEvent} is the one event this application delivers both ways. It reaches
 * {@code ProjectParticipantCacheEvictionListener} inline, inside the publishing transaction, because
 * a stale participant must not be readable; it reaches this class over the topic, because a
 * membership row is audit and audit must not be able to fail the request. Externalising an event
 * does not stop it being delivered in process, so both hold at once.
 */
@Component
@KafkaListener(topics = "project-events", groupId = "activity-project-writer")
@RequiredArgsConstructor
class ProjectActivityListener {

  private static final Logger log = LoggerFactory.getLogger(ProjectActivityListener.class);

  private final ProjectActivityRepository projectActivityRepository;

  @KafkaHandler
  void on(ProjectCreatedEvent event) {
    record(
        event.projectId(), event.actorId(), ProjectActionType.CREATED, null, null, event.occurredAt());
  }

  @KafkaHandler
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

  @KafkaHandler
  void on(ProjectDeletedEvent event) {
    record(
        event.projectId(), event.actorId(), ProjectActionType.DELETED, null, null, event.occurredAt());
  }

  /**
   * The subject goes into {@code newValue} when joining and {@code oldValue} when leaving, so the two
   * columns keep saying "what it was" and "what it is" - see {@link ProjectActivity}.
   */
  @KafkaHandler
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

  /** See {@code IssueActivityListener#unknown}, including why this is a warning. */
  @KafkaHandler(isDefault = true)
  void unknown(Object payload) {
    log.warn(
        "Dropping unhandled payload on project-events: {}. Nothing was written to"
            + " project_activities.",
        payload == null ? "null" : payload.getClass().getName());
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
