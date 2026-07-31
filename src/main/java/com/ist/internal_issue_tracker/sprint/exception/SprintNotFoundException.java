package com.ist.internal_issue_tracker.sprint.exception;

import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;

/**
 * No live sprint with the given id <em>on the given project</em>. A sprint that exists but belongs
 * to a different project reports as missing rather than forbidden: the caller has no business
 * knowing it is there, and a 403 would confirm it.
 */
public class SprintNotFoundException extends ResourceNotFoundException {

  public SprintNotFoundException(Integer sprintId) {
    super(SprintErrorCode.SPRINT_NOT_FOUND, "Sprint with id " + sprintId + " was not found");
  }
}
