package com.ist.internal_issue_tracker.project.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ProjectErrorCode implements ErrorCode {
  PROJECT_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "This project name already exists."),
  PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "This project does not exist."),
  LEADER_NOT_FOUND(
      HttpStatus.UNPROCESSABLE_ENTITY, "The given leader does not exist or is not active."),
  PROJECT_STATUS_NOT_DEFINED(
      HttpStatus.UNPROCESSABLE_ENTITY, "This status is not one of the defined project statuses.");

  private final HttpStatus httpStatus;
  private final String message;

  ProjectErrorCode(HttpStatus httpStatus, String message) {
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
