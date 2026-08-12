package com.ist.internal_issue_tracker.activity;

/**
 * Which of the three activity tables a row came from, and what {@code ActivityResponse.subjectId}
 * means on it: a project id, an issue id, or a sprint id. Exists mainly to give the string literal
 * the union query in {@link ProjectActivityRepository} embeds a single named source of truth,
 * rather than "PROJECT"/"ISSUE"/"SPRINT" typed out separately in SQL and in {@code ActivityMapper}.
 */
public enum ActivityScope {
  PROJECT,
  ISSUE,
  SPRINT
}
