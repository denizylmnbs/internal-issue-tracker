package com.ist.internal_issue_tracker.issue;

/** Which unit resolves the issue. Mirrors the {@code CHECK} on {@code issues.resolving_unit} exactly. */
public enum IssueUnit {
  BACKEND,
  FRONTEND,
  IOS,
  ANDROID
}
