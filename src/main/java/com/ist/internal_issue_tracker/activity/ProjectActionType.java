package com.ist.internal_issue_tracker.activity;

/**
 * Mirrors the {@code CHECK} on {@code project_activities.action_type} exactly - see {@link
 * IssueActionType} for why this is kept apart from the {@code ProjectField} the publisher speaks.
 */
public enum ProjectActionType {
  CREATED,
  LEADER_UPDATED,
  TEAM_ADDED,
  TEAM_REMOVED,
  USER_ADDED,
  USER_REMOVED,
  DETAILS_UPDATED,
  STATUS_UPDATED,
  DELETED
}
