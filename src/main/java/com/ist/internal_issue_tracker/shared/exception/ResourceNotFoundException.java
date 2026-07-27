package com.ist.internal_issue_tracker.shared.exception;

/**
 * The requested resource does not exist, or exists but is soft-deleted ({@code is_active = false}).
 */
public class ResourceNotFoundException extends AppException {

  public ResourceNotFoundException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  /** {@code ResourceNotFoundException.of("Issue", 42)} -> "Issue with id 42 was not found". */
  public static ResourceNotFoundException of(String resourceType, Object id) {
    return new ResourceNotFoundException(
        CommonErrorCode.RESOURCE_NOT_FOUND, resourceType + " with id " + id + " was not found");
  }
}
