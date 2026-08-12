package com.ist.internal_issue_tracker.epic.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum EpicErrorCode implements ErrorCode {
  EPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "This epic does not exist."),
  EPIC_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "This project already has an epic by that name."),
  PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "This project does not exist."),
  EPIC_STATUS_NOT_DEFINED(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "This status is not one of the project's defined epic statuses.");

  private final HttpStatus httpStatus;
  private final String message;

  EpicErrorCode(HttpStatus httpStatus, String message) {
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
