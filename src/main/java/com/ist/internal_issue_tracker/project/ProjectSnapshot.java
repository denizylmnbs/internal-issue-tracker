package com.ist.internal_issue_tracker.project;

import java.time.LocalDate;

/**
 * The audited fields of a project, copied off it before a write - see {@code IssueSnapshot} for why
 * a copy is the only way the old values survive the update.
 */
record ProjectSnapshot(
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    Integer leaderId,
    ProjectStatus status) {

  static ProjectSnapshot of(Project project) {
    return new ProjectSnapshot(
        project.getName(),
        project.getDescription(),
        project.getStartDate(),
        project.getEndDate(),
        project.getLeaderId(),
        project.getStatus());
  }
}
