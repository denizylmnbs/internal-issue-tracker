package com.ist.internal_issue_tracker.activity;

/**
 * Mirrors the {@code CHECK} on {@code sprint_activities.action_type} exactly - see {@link
 * IssueActionType} for why this is kept apart from the {@code SprintField} the publisher speaks.
 */
public enum SprintActionType {
  CREATED,
  STATUS_UPDATED,
  DATES_UPDATED,
  DETAILS_UPDATED,
  DELETED
}
