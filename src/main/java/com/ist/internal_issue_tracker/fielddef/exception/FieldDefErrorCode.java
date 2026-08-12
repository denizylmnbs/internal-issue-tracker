package com.ist.internal_issue_tracker.fielddef.exception;

import com.ist.internal_issue_tracker.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Errors from managing field definitions themselves (creating, editing, reordering, retiring a
 * status/type/priority/etc.). Not to be confused with the "this code isn't one of ours" error a
 * consuming module (e.g. {@code issue}) raises when a caller writes an undefined code onto its own
 * entity - that error belongs to the consuming module's own {@code ErrorCode} enum, the same way
 * every other cross-module validation failure in this app is reported by the module doing the
 * validating, not by the module being asked the question.
 */
public enum FieldDefErrorCode implements ErrorCode {
  FIELD_DEFINITION_NOT_FOUND(HttpStatus.NOT_FOUND, "This field definition does not exist."),
  FIELD_CODE_ALREADY_EXISTS(
      HttpStatus.CONFLICT, "This kind already has a field definition with that code."),
  LAST_DONE_FIELD_REQUIRED(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "At least one active field definition of this kind must be marked as done."),
  DEFAULT_FIELD_REQUIRED(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "Exactly one active field definition of this kind must be the default."),
  FIELD_KIND_NOT_PROJECT_SCOPED(
      HttpStatus.UNPROCESSABLE_ENTITY, "This kind is managed globally, not per project."),
  FIELD_KIND_NOT_GLOBAL(
      HttpStatus.UNPROCESSABLE_ENTITY, "This kind is managed per project, not globally."),
  REORDER_SET_MISMATCH(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "The ids given do not match exactly the active field definitions of this kind.");

  private final HttpStatus httpStatus;
  private final String message;

  FieldDefErrorCode(HttpStatus httpStatus, String message) {
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
