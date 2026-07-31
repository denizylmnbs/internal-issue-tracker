package com.ist.internal_issue_tracker.sprint;

/**
 * Where a sprint is in its life. Mirrors the {@code CHECK} on {@code sprints.status} exactly - a
 * name added here without the migration agreeing would only fail at write time.
 *
 * <p>Unrelated to deletion: a sprint is removed by stamping {@code deletedAt}, never by moving it to
 * a status. {@link #COMPLETED} is a finished sprint, not a gone one.
 */
public enum SprintStatus {
  TODO,
  IN_PROGRESS,
  TESTING,
  COMPLETED
}
