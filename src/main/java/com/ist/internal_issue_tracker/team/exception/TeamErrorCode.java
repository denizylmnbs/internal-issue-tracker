package com.ist.internal_issue_tracker.team.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TeamErrorCode implements ErrorCode {
  TEAM_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "This team name already exists."),
  LEADER_NOT_FOUND(
      HttpStatus.UNPROCESSABLE_ENTITY, "The given leader does not exist or is not active.");

  private final HttpStatus httpStatus;
  private final String message;

  TeamErrorCode(HttpStatus httpStatus, String message) {
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
