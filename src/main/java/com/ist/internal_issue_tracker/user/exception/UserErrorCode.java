package com.ist.internal_issue_tracker.user.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {
  EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "This email already exists."),
  CURRENT_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "Current password is incorrect."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."),
  ROLE_CHANGE_NOT_PERMITTED(HttpStatus.FORBIDDEN, "Changing user role is not permitted.");

  private final HttpStatus httpStatus;
  private final String message;

  UserErrorCode(HttpStatus httpStatus, String message) {
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
