package com.ist.internal_issue_tracker.fielddef.exception;

import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;

public class FieldDefinitionNotFoundException extends ResourceNotFoundException {
  public FieldDefinitionNotFoundException(Integer id) {
    super(
        FieldDefErrorCode.FIELD_DEFINITION_NOT_FOUND,
        "Field definition with id " + id + " was not found");
  }
}
