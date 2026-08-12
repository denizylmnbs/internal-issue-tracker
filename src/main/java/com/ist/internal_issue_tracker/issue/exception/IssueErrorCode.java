package com.ist.internal_issue_tracker.issue.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * The four reference codes are {@code 422} rather than {@code 404}, following {@code
 * ProjectErrorCode.LEADER_NOT_FOUND}: the request is well formed and the issue it addresses may
 * well exist - it is the thing being pointed at that cannot be used. A {@code 404} would say the
 * issue itself was missing, which is a different problem with a different fix.
 */
public enum IssueErrorCode implements ErrorCode {
  ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "This issue does not exist."),
  PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "This project does not exist."),
  SPRINT_NOT_FOUND(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "The given sprint does not exist or does not belong to this project."),
  EPIC_NOT_FOUND(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "The given epic does not exist or does not belong to this project."),
  ASSIGNEE_USER_NOT_FOUND(
      HttpStatus.UNPROCESSABLE_ENTITY, "The given assignee does not exist or is not active."),
  ASSIGNEE_TEAM_NOT_FOUND(
      HttpStatus.UNPROCESSABLE_ENTITY, "The given assignee team does not exist or is not active."),
  ISSUE_STATUS_NOT_DEFINED(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "This status is not one of the project's defined issue statuses."),
  ISSUE_TYPE_NOT_DEFINED(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "This type is not one of the project's defined issue types."),
  ISSUE_PRIORITY_NOT_DEFINED(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "This priority is not one of the project's defined issue priorities."),
  ISSUE_UNIT_NOT_DEFINED(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "This resolving unit is not one of the project's defined issue units.");

  private final HttpStatus httpStatus;
  private final String message;

  IssueErrorCode(HttpStatus httpStatus, String message) {
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
