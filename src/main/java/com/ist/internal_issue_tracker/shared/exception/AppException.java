package com.ist.internal_issue_tracker.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for every application-level (as opposed to framework-level)
 * exception. Carries an {@link ErrorCode}, which is the single source of
 * truth for both the HTTP status and the machine-readable error code —
 * never annotate a subclass with {@code @ResponseStatus}, it would create a
 * second, driftable source of truth.
 * <p>
 * Not abstract: most errors deserve a dedicated subclass (see
 * {@link ResourceNotFoundException}, {@link BusinessRuleViolationException},
 * etc.), but a module may throw {@code AppException} directly for a one-off
 * error code without minting a class just for typing purposes.
 */
public class AppException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String code() {
        return errorCode.code();
    }

    public HttpStatus status() {
        return errorCode.status();
    }
}
