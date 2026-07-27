package com.ist.internal_issue_tracker.shared.exception;

/**
 * A resource with the given unique attributes already exists — the application-level pre-check
 * counterpart of a partial unique index (e.g. {@code unique_active_sprint_name_per_project}, {@code
 * users.email}). Maps to 409.
 */
public class DuplicateResourceException extends AppException {

  public DuplicateResourceException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
