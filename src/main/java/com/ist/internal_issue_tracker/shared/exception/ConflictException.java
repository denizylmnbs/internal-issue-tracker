package com.ist.internal_issue_tracker.shared.exception;

/**
 * The request conflicts with the current state of the resource, but is not a
 * duplicate (e.g. {@code one_active_sprint_per_project} — starting a sprint
 * while another is already {@code IN_PROGRESS}). Maps to 409.
 */
public class ConflictException extends AppException {

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
