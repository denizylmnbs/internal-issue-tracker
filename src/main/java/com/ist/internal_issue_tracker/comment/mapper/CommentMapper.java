package com.ist.internal_issue_tracker.comment.mapper;

import com.ist.internal_issue_tracker.comment.Comment;
import com.ist.internal_issue_tracker.comment.dto.CommentCreateRequest;
import com.ist.internal_issue_tracker.comment.dto.CommentResponse;
import com.ist.internal_issue_tracker.comment.dto.CommentUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

  /** The issue comes from the path and the author from the authenticated caller. */
  public Comment toEntity(Integer issueId, Integer userId, CommentCreateRequest request) {
    Comment comment = new Comment();

    comment.setIssueId(issueId);
    comment.setUserId(userId);
    comment.setContent(request.content());

    return comment;
  }

  /** Only the content. The author and the issue are not this method's to touch, nor anyone's. */
  public void updateEntity(Comment comment, CommentUpdateRequest request) {
    comment.setContent(request.content());
  }

  public CommentResponse toResponse(Comment comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getIssueId(),
        comment.getUserId(),
        comment.getContent(),
        comment.getCreatedAt(),
        comment.getUpdatedAt());
  }
}
