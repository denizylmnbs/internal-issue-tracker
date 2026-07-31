package com.ist.internal_issue_tracker.epic.exception;

import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;

/**
 * No live epic with the given id <em>on the given project</em>. An epic that exists but belongs to a
 * different project reports as missing rather than forbidden: the caller has no business knowing it
 * is there, and a 403 would confirm it.
 */
public class EpicNotFoundException extends ResourceNotFoundException {

  public EpicNotFoundException(Integer epicId) {
    super(EpicErrorCode.EPIC_NOT_FOUND, "Epic with id " + epicId + " was not found");
  }
}
