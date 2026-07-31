package com.ist.internal_issue_tracker.issue;

/**
 * What kind of work the issue is. Mirrors the {@code CHECK} on {@code issues.type} exactly.
 *
 * <p>The column is nullable but the API is not - see {@code IssueCreateRequest}.
 */
public enum IssueType {
  BUG,
  FEATURE,
  STORY,
  TASK,
  ENHANCEMENT,
  REFACTOR
}
