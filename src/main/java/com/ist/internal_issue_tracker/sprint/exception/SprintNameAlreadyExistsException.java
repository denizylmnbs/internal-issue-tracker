package com.ist.internal_issue_tracker.sprint.exception;

import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;

/**
 * Another live sprint on the same project already uses the name. Scoped to the project, and only to
 * sprints that have not been deleted - {@code unique_active_sprint_name_per_project} is partial.
 */
public class SprintNameAlreadyExistsException extends DuplicateResourceException {

  public SprintNameAlreadyExistsException(String name) {
    super(
        SprintErrorCode.SPRINT_NAME_ALREADY_EXISTS,
        "This project already has a sprint named: " + name);
  }
}
