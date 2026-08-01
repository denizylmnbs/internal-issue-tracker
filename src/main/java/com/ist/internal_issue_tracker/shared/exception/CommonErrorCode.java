package com.ist.internal_issue_tracker.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Framework-level / cross-cutting error codes that don't belong to any single business module.
 * Module-specific codes (e.g. {@code SPRINT_ALREADY_IN_PROGRESS}) live in their own module as
 * separate {@link ErrorCode} enums.
 */
public enum CommonErrorCode implements ErrorCode {
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
  MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Request body could not be parsed"),
  TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "A request parameter has the wrong type"),
  MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "A required request parameter is missing"),
  INVALID_SORT_PROPERTY(HttpStatus.BAD_REQUEST, "The requested sort property does not exist"),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "You are not allowed to perform this action"),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found"),
  ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "No endpoint matches this request"),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint"),
  NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "No acceptable representation available"),
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type"),
  CONFLICT(HttpStatus.CONFLICT, "The request conflicts with the current state"),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "A resource with these values already exists"),
  BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "The request violates a business rule"),
  PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Request payload is too large"),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

  private final HttpStatus status;
  private final String defaultMessage;

  CommonErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  @Override
  public String code() {
    return name();
  }

  @Override
  public HttpStatus status() {
    return status;
  }

  @Override
  public String defaultMessage() {
    return defaultMessage;
  }
}
