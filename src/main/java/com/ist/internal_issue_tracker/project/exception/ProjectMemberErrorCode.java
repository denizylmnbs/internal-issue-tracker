package com.ist.internal_issue_tracker.project.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * A missing project is reported through {@link ProjectNotFoundException} rather than a code of its
 * own here, so the two ids in these requests stay distinguishable in the response.
 */
public enum ProjectMemberErrorCode implements ErrorCode {
  USER_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "This user not exist."),
  USER_ROLE_NOT_ENOUGH(HttpStatus.FORBIDDEN, "User role is not enough to perform this action."),
  PROJECT_MEMBER_ALREADY_EXIST(HttpStatus.CONFLICT, "This user already exist in this project."),
  PROJECT_MEMBER_NOT_FOUND(
      HttpStatus.NOT_FOUND, "This user is not an active member of this project.");

  private final HttpStatus httpStatus;
  private final String message;

  ProjectMemberErrorCode(HttpStatus httpStatus, String message) {
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
