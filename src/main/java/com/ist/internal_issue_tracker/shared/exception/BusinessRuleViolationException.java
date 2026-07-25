package com.ist.internal_issue_tracker.shared.exception;

/**
 * The request is well-formed and passed bean validation, but violates a domain
 * rule (e.g. an invalid status transition, {@code end_date < start_date}).
 * Maps to 422, not 400 — 400 is reserved for requests that couldn't even be
 * parsed/bound.
 */
public class BusinessRuleViolationException extends AppException {

    public BusinessRuleViolationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
