package com.ist.internal_issue_tracker.sprint.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SprintErrorCode implements ErrorCode {
  SPRINT_NOT_FOUND(HttpStatus.NOT_FOUND, "This sprint does not exist."),
  SPRINT_NAME_ALREADY_EXISTS(
      HttpStatus.CONFLICT, "This project already has a sprint by that name."),
  PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "This project does not exist."),
  /** Enforced by {@code one_active_sprint_per_project}, not by a rule of our own. */
  SPRINT_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "This project already has a sprint in progress.");

  private final HttpStatus httpStatus;
  private final String message;

  SprintErrorCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }

  @Override
  public HttpStatus status() {
    return httpStatus;
  }

  @Override
  public String defaultMessage() {
    return message;
  }

  @Override
  public String code() {
    return name();
  }
}
