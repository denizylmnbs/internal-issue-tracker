package com.ist.internal_issue_tracker.comment;

import com.ist.internal_issue_tracker.comment.dto.CommentCreateRequest;
import com.ist.internal_issue_tracker.comment.dto.CommentResponse;
import com.ist.internal_issue_tracker.comment.dto.CommentUpdateRequest;
import com.ist.internal_issue_tracker.comment.exception.CommentErrorCode;
import com.ist.internal_issue_tracker.comment.exception.CommentNotFoundException;
import com.ist.internal_issue_tracker.comment.mapper.CommentMapper;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.IssueLookup;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.security.Role;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Authorization here is split across two layers on purpose. {@code SecurityConfig} is the coarse
 * gate - someone who does not work on the project never reaches these methods at all. This class is
 * the fine one: of the people who did get in, which may touch <em>this</em> comment. The second
 * question cannot be answered by a request matcher, because the answer is a column.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;
  private final ProjectLookup projectLookup;
  private final IssueLookup issueLookup;

  /**
   * Three links, and all three are load-bearing.
   *
   * <p>The project is checked first and separately rather than folded into the issue check, because
   * an issue row does not know its project has been soft-deleted - {@code existsLiveIssueInProject}
   * would happily confirm an issue on a dead project. Asking about the project first is what makes
   * a deleted project's comment threads unreachable, the same structural guard that covers sprints
   * and epics.
   */
  private void requireLiveIssue(Integer projectId, Integer issueId) {
    if (!projectLookup.existsActiveProject(projectId)) {
      throw new AppException(CommentErrorCode.PROJECT_NOT_FOUND);
    }

    if (!issueLookup.existsLiveIssueInProject(projectId, issueId)) {
      throw new AppException(CommentErrorCode.ISSUE_NOT_FOUND);
    }
  }

  /** Both keys - see {@code CommentRepository#findByIdAndIssueIdAndDeletedAtIsNull}. */
  private Comment requireLiveComment(Integer issueId, Integer commentId) {
    return commentRepository
        .findByIdAndIssueIdAndDeletedAtIsNull(commentId, issueId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));
  }

  /**
   * Editing is the author's alone. Nobody - not an editor, not the project's leader, not an admin -
   * gets to change what someone else said and leave their name on it.
   */
  private void requireAuthor(Comment comment, AuthenticatedUser caller) {
    if (!comment.getUserId().equals(caller.getId())) {
      throw new AppException(CommentErrorCode.COMMENT_NOT_OWNED);
    }
  }

  /**
   * Deleting is wider, because removing a remark is not the same as putting words in someone's
   * mouth: the author may retract, and an editor or the project's leader may moderate.
   *
   * <p>The leadership question is asked last on purpose - it is the only one of the three that
   * costs a query, and the two cheap checks answer it for most callers first.
   */
  private void requireAuthorOrModerator(
      Integer projectId, Comment comment, AuthenticatedUser caller) {
    boolean allowed =
        comment.getUserId().equals(caller.getId())
            || caller.getRole().atLeast(Role.EDITOR)
            || projectLookup.isLeaderOfProject(projectId, caller.getId());

    if (!allowed) {
      throw new AppException(CommentErrorCode.COMMENT_NOT_OWNED);
    }
  }

  /** {@code userId} is the caller - see {@code EpicService#createEpic} for why it is trusted. */
  public CommentResponse createComment(
      Integer projectId, Integer issueId, Integer userId, CommentCreateRequest request) {
    requireLiveIssue(projectId, issueId);

    Comment comment = commentMapper.toEntity(issueId, userId, request);

    return commentMapper.toResponse(commentRepository.save(comment));
  }

  public CommentResponse getCommentById(Integer projectId, Integer issueId, Integer commentId) {
    requireLiveIssue(projectId, issueId);

    return commentMapper.toResponse(requireLiveComment(issueId, commentId));
  }

  public PagedResponse<CommentResponse> getCommentsByIssueId(
      Integer projectId, Integer issueId, Integer userId, Pageable pageable) {
    requireLiveIssue(projectId, issueId);

    // derived query, so the caller's sort is honoured as-is
    Page<Comment> comments = commentRepository.findAllByFilters(issueId, userId, pageable);

    return PagedResponse.from(comments.map(commentMapper::toResponse));
  }

  public CommentResponse updateComment(
      Integer projectId,
      Integer issueId,
      Integer commentId,
      CommentUpdateRequest request,
      AuthenticatedUser caller) {
    requireLiveIssue(projectId, issueId);

    Comment comment = requireLiveComment(issueId, commentId);

    requireAuthor(comment, caller);

    commentMapper.updateEntity(comment, request);

    return commentMapper.toResponse(commentRepository.save(comment));
  }

  /** Soft delete: the row stays and {@code deletedAt} is stamped. */
  public void deleteComment(
      Integer projectId, Integer issueId, Integer commentId, AuthenticatedUser caller) {
    requireLiveIssue(projectId, issueId);

    Comment comment = requireLiveComment(issueId, commentId);

    requireAuthorOrModerator(projectId, comment, caller);

    comment.setDeletedAt(OffsetDateTime.now());

    commentRepository.save(comment);
  }
}
