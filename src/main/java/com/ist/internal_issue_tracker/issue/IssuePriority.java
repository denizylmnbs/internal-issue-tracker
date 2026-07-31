package com.ist.internal_issue_tracker.issue;

/** How urgent the issue is. Mirrors the {@code CHECK} on {@code issues.priority} exactly. */
public enum IssuePriority {
  LOW,
  MEDIUM,
  HIGH,
  CRITICAL
}
