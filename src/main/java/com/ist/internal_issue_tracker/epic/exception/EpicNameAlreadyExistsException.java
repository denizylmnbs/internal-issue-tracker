package com.ist.internal_issue_tracker.epic.exception;

import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;

/**
 * Another live epic on the same project already uses the name. Scoped to the project, and only to
 * epics that have not been deleted - {@code unique_active_epic_name_per_project} is partial.
 */
public class EpicNameAlreadyExistsException extends DuplicateResourceException {

  public EpicNameAlreadyExistsException(String name) {
    super(
        EpicErrorCode.EPIC_NAME_ALREADY_EXISTS, "This project already has an epic named: " + name);
  }
}
