package com.ist.internal_issue_tracker.activity.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * All {@code 404}, unlike {@code IssueErrorCode}'s reference codes: here the thing being addressed
 * <em>is</em> the subject of the request rather than something it points at, so a missing issue
 * means the history being asked for does not exist.
 */
public enum ActivityErrorCode implements ErrorCode {
  PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "This project does not exist."),
  ISSUE_NOT_FOUND(
      HttpStatus.NOT_FOUND, "This issue does not exist or does not belong to this project."),
  SPRINT_NOT_FOUND(
      HttpStatus.NOT_FOUND, "This sprint does not exist or does not belong to this project.");

  private final HttpStatus httpStatus;
  private final String message;

  ActivityErrorCode(HttpStatus httpStatus, String message) {
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
