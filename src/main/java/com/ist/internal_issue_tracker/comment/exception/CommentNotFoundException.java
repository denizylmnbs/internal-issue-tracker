package com.ist.internal_issue_tracker.comment.exception;

import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;

/**
 * No live comment with the given id <em>on the given issue</em>. A comment that exists but hangs
 * off a different issue reports as missing rather than forbidden - the caller has no business
 * knowing it is there.
 */
public class CommentNotFoundException extends ResourceNotFoundException {

  public CommentNotFoundException(Integer commentId) {
    super(CommentErrorCode.COMMENT_NOT_FOUND, "Comment with id " + commentId + " was not found");
  }
}
