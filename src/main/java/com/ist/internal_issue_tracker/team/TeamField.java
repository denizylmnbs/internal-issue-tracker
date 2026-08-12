package com.ist.internal_issue_tracker.team;

/**
 * Discipline a team works in. Persisted as a string, so the constant names must stay in sync with
 * the {@code teams.field} CHECK constraint in the schema - adding a value here requires a Flyway
 * migration that widens that constraint.
 */
public enum
TeamField {
  BACKEND,
  FRONTEND,
  ANDROID,
  IOS,
  DESIGN,
  DATA
}
