package com.ist.internal_issue_tracker.comment.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CommentErrorCode implements ErrorCode {
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "This comment does not exist."),
  ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "This issue does not exist."),
  PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "This project does not exist."),
  /**
   * {@code 403} rather than {@code 404}, following {@code UserErrorCode.ROLE_CHANGE_NOT_PERMITTED}:
   * the comment exists and the caller can already see it in the listing, so hiding it now would
   * fool nobody and only confuse a legitimate client. What they lack is the right to change it.
   */
  COMMENT_NOT_OWNED(HttpStatus.FORBIDDEN, "This comment belongs to someone else.");

  private final HttpStatus httpStatus;
  private final String message;

  CommentErrorCode(HttpStatus httpStatus, String message) {
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
