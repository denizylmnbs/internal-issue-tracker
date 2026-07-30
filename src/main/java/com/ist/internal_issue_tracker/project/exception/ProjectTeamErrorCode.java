package com.ist.internal_issue_tracker.project.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** See {@link ProjectMemberErrorCode} for why a missing project has no code here. */
public enum ProjectTeamErrorCode implements ErrorCode {
  TEAM_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "This team not exist."),
  PROJECT_TEAM_ALREADY_EXIST(HttpStatus.CONFLICT, "This team already exist in this project."),
  PROJECT_TEAM_NOT_FOUND(
      HttpStatus.NOT_FOUND, "This team is not actively assigned to this project.");

  private final HttpStatus httpStatus;
  private final String message;

  ProjectTeamErrorCode(HttpStatus httpStatus, String message) {
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
