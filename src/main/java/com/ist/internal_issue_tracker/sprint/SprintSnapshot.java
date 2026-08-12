package com.ist.internal_issue_tracker.sprint;

import java.time.LocalDate;

/**
 * The audited fields of a sprint, copied off it before a write - see {@code IssueSnapshot} for why
 * a copy is the only way the old values survive the update.
 */
record SprintSnapshot(
    String name, String description, LocalDate startDate, LocalDate endDate, SprintStatus status) {

  static SprintSnapshot of(Sprint sprint) {
    return new SprintSnapshot(
        sprint.getName(),
        sprint.getDescription(),
        sprint.getStartDate(),
        sprint.getEndDate(),
        sprint.getStatus());
  }
}
