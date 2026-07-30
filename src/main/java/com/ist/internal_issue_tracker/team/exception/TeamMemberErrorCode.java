package com.ist.internal_issue_tracker.team.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TeamMemberErrorCode implements ErrorCode {
  USER_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "This user not exist."),
  USER_ROLE_NOT_ENOUGH(HttpStatus.FORBIDDEN, "User role is not enough to perform this action."),
  TEAM_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "This team not exist."),
  TEAM_MEMBER_ALREADY_EXIST(HttpStatus.CONFLICT, "This user already exist in this team."),
  TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "This user is not an active member of this team.");

  private final HttpStatus httpStatus;
  private final String message;

  TeamMemberErrorCode(HttpStatus httpStatus, String message) {
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
