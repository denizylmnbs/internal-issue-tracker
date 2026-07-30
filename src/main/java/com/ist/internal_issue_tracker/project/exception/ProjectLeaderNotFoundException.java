package com.ist.internal_issue_tracker.project.exception;

import com.ist.internal_issue_tracker.shared.exception.AppException;

/** The candidate leader does not exist or is no longer active, so it cannot lead a project. */
public class ProjectLeaderNotFoundException extends AppException {

  public ProjectLeaderNotFoundException(Integer leaderId) {
    super(ProjectErrorCode.LEADER_NOT_FOUND, "User with id " + leaderId + " cannot lead a project");
  }
}
