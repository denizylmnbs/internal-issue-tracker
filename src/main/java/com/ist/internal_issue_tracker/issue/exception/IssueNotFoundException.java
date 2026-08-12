package com.ist.internal_issue_tracker.issue.exception;

import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;

/**
 * No live issue with the given id <em>on the given project</em>. An issue that exists but belongs
 * to a different project reports as missing rather than forbidden: the caller has no business
 * knowing it is there, and a 403 would confirm it.
 */
public class IssueNotFoundException extends ResourceNotFoundException {

  public IssueNotFoundException(Integer issueId) {
    super(IssueErrorCode.ISSUE_NOT_FOUND, "Issue with id " + issueId + " was not found");
  }
}
