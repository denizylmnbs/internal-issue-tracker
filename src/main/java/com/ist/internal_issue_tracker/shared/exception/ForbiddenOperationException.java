package com.ist.internal_issue_tracker.shared.exception;

/**
 * A data-dependent authorization rule was violated (e.g. "is this user the
 * leader of this project?" — {@code projects.leader_id} / {@code teams.leader_id}).
 * Distinct from Spring Security's {@code AccessDeniedException}, which answers
 * "does this principal hold this static authority" and is thrown by the
 * security infrastructure, not by domain code. Maps to 403.
 */
public class ForbiddenOperationException extends AppException {

    public ForbiddenOperationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
