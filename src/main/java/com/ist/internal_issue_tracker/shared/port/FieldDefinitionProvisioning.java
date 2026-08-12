package com.ist.internal_issue_tracker.shared.port;

/**
 * Materialising a new project's copy of the project-scoped classification kinds.
 *
 * <p>The only write port in {@code shared.port} - every other port in this package only answers
 * questions. {@code project} calls {@link #seedDefaults} in the same transaction as {@code
 * ProjectService#createProject}, so a seeding failure fails the project creation rather than
 * leaving a project with no status vocabulary to move issues, sprints or epics through.
 */
public interface FieldDefinitionProvisioning {

  /**
   * Copies the current global default rows of every project-scoped {@link FieldKind} (everything
   * except {@link FieldKind#PROJECT_STATUS} and {@link FieldKind#TEAM_FIELD}) into rows owned by
   * this project. Idempotent is not a concern - this is called exactly once, from project
   * creation.
   */
  void seedDefaults(Integer projectId);
}
