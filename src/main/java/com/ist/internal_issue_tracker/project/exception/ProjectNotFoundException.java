package com.ist.internal_issue_tracker.project.exception;

import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;

/**
 * No project with the given id. Extends {@link ResourceNotFoundException} so it still reads as a
 * missing resource to anything handling that base type, but carries {@code PROJECT_NOT_FOUND}
 * instead of the generic {@code RESOURCE_NOT_FOUND} code, letting a client tell which of several
 * ids in a request was the bad one.
 */
public class ProjectNotFoundException extends ResourceNotFoundException {

  public ProjectNotFoundException(Integer projectId) {
    super(ProjectErrorCode.PROJECT_NOT_FOUND, "Project with id " + projectId + " was not found");
  }
}
