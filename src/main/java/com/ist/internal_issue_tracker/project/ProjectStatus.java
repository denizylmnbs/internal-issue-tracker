package com.ist.internal_issue_tracker.project;

/**
 * Where a project stands in its lifecycle. Persisted as a string, so the constant names must stay in
 * sync with the {@code projects.status} CHECK constraint in the schema - adding a value here
 * requires a Flyway migration that widens that constraint.
 *
 * <p>Distinct from {@code projects.is_active}: this says what is happening to a project, that says
 * whether the record still exists at all.
 */
public enum ProjectStatus {
  /** Created but not started; the default a project is born in. */
  PLANNING,
  ACTIVE,
  ON_HOLD,
  COMPLETED,
  CANCELLED
}
