package com.ist.internal_issue_tracker.shared.storage;

/**
 * Wraps an unexpected object-storage failure (network, credentials, a bug in this layer). Not an
 * {@link com.ist.internal_issue_tracker.shared.exception.AppException}: {@code
 * GlobalExceptionHandler} treats a 5xx {@code AppException} as a mapping bug in the throwing code
 * and replaces it with a trace id, which is exactly right for an infrastructure failure that has
 * nothing to do with the caller's request - it belongs on the generic catch-all path, not a
 * bespoke error code.
 */
public class ObjectStorageException extends RuntimeException {

  public ObjectStorageException(String message, Throwable cause) {
    super(message, cause);
  }

  public ObjectStorageException(String message) {
    super(message);
  }
}
