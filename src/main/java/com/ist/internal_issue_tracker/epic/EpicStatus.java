package com.ist.internal_issue_tracker.epic;

/**
 * Where an epic stands. Mirrors the {@code CHECK} on {@code epics.status} exactly - a name added
 * here without the migration agreeing would only fail at write time.
 *
 * <p>Richer than {@code SprintStatus}: an epic can be parked ({@link #ON_HOLD}) or abandoned ({@link
 * #CANCELLED}), neither of which a sprint has a word for. None of it is deletion - an epic is
 * removed by stamping {@code deletedAt}, and {@link #CANCELLED} is a decision, not a disappearance.
 */
public enum EpicStatus {
  TODO,
  IN_PROGRESS,
  ON_HOLD,
  COMPLETED,
  CANCELLED
}
