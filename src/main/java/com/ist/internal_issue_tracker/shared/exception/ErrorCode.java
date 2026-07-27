package com.ist.internal_issue_tracker.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Contract every module-specific error code enum must implement. Keeps {@code shared} free of
 * domain vocabulary: modules depend on this interface, {@code shared} never depends on a module's
 * enum.
 */
public interface ErrorCode {

  /** Stable, machine-readable identifier exposed as {@code ApiError.code}. */
  String code();

  HttpStatus status();

  /** Fallback message used when the throw site does not supply one. */
  default String defaultMessage() {
    return code();
  }
}
