package com.ist.internal_issue_tracker.project.exception;

import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;

/** Another project already uses the given name — {@code projects.name} is unique. */
public class ProjectNameAlreadyExistsException extends DuplicateResourceException {

  public ProjectNameAlreadyExistsException(String name) {
    super(ProjectErrorCode.PROJECT_NAME_ALREADY_EXISTS, "This project name already exists: " + name);
  }
}
