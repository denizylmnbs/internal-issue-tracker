package com.ist.internal_issue_tracker.fielddef.exception;

import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;
import com.ist.internal_issue_tracker.shared.port.FieldKind;

public class FieldCodeAlreadyExistsException extends DuplicateResourceException {
  public FieldCodeAlreadyExistsException(FieldKind kind, String code) {
    super(
        FieldDefErrorCode.FIELD_CODE_ALREADY_EXISTS,
        kind + " already has a field definition with code '" + code + "'");
  }
}
