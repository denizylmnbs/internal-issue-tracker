package com.ist.internal_issue_tracker.activity;

/**
 * Mirrors the {@code CHECK} on {@code issue_activities.action_type} exactly - a name added here
 * without the migration agreeing would only fail at write time.
 *
 * <p>This is the storage vocabulary. What the publishing module says is {@code IssueField}, and the
 * two are joined in one {@code switch} in {@link IssueActivityListener}. They are close enough to
 * look redundant and are kept apart on purpose: this one is bound to a database constraint, the
 * other to what {@code issue} considers a change, and neither should have to move when the other
 * does.
 *
 * <p>{@code CREATED} and {@code DELETED} have no counterpart in {@code IssueField} because they are
 * not field changes - they bound the issue's life rather than happening within it.
 */
public enum IssueActionType {
  CREATED,
  STATUS_UPDATED,
  PRIORITY_UPDATED,
  ASSIGNEE_USER_UPDATED,
  ASSIGNEE_TEAM_UPDATED,
  SPRINT_UPDATED,
  STORY_POINT_UPDATED,
  DETAILS_UPDATED,
  DELETED
}
