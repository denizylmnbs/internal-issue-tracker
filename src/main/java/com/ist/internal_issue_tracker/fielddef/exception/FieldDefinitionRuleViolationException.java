package com.ist.internal_issue_tracker.fielddef.exception;

import com.ist.internal_issue_tracker.shared.exception.BusinessRuleViolationException;

/** {@link FieldDefErrorCode#LAST_DONE_FIELD_REQUIRED}, {@code DEFAULT_FIELD_REQUIRED}, etc. */
public class FieldDefinitionRuleViolationException extends BusinessRuleViolationException {
  public FieldDefinitionRuleViolationException(FieldDefErrorCode errorCode) {
    super(errorCode, errorCode.defaultMessage());
  }
}
