package com.ist.internal_issue_tracker.shared.port;

/**
 * The eight classification points a project (or, for {@link #PROJECT_STATUS} and {@link
 * #TEAM_FIELD}, the whole instance) can define its own values for.
 *
 * <p>This enum is what stays fixed while the values under each kind become user-defined rows in
 * {@code field_definitions}. It lives in {@code shared} rather than in {@code fielddef} because
 * every module that writes one of these classifications onto its own entity - {@code issue},
 * {@code sprint}, {@code epic}, {@code project}, {@code team} - needs to name a kind when it asks
 * {@link FieldDefinitionLookup} whether a code is valid, and none of those modules may depend on
 * {@code fielddef} to do so.
 */
public enum FieldKind {
  PROJECT_STATUS,
  SPRINT_STATUS,
  EPIC_STATUS,
  ISSUE_STATUS,
  ISSUE_TYPE,
  ISSUE_PRIORITY,
  ISSUE_UNIT,
  TEAM_FIELD;

  /**
   * {@code true} for the two kinds that are not scoped to a project - {@link #PROJECT_STATUS}
   * (a project cannot own the vocabulary used to list projects) and {@link #TEAM_FIELD} (a team
   * does not belong to a project at all).
   */
  public boolean isGlobal() {
    return this == PROJECT_STATUS || this == TEAM_FIELD;
  }
}
