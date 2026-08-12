package com.ist.internal_issue_tracker.comment;

import com.ist.internal_issue_tracker.comment.dto.CommentCreateRequest;
import com.ist.internal_issue_tracker.comment.dto.CommentResponse;
import com.ist.internal_issue_tracker.comment.dto.CommentUpdateRequest;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * The deepest routes in the application: a comment hangs off an issue, which hangs off a project.
 * The project still owns the {@code id} name, which is what lets {@code editorLeaderOrParticipant}
 * cover these routes unchanged - the alternative was a shallower URL plus a port and an
 * authorization manager to go with it.
 *
 * <p>{@code caller} is passed down on write operations because who may edit a comment depends on
 * who wrote it, and that is a fact the request path cannot carry.
 */
@RestController
@RequestMapping("/api/projects/{id}/issues/{issueId}/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @PostMapping
  public ResponseEntity<ApiResponse<CommentResponse>> createComment(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @PathVariable Integer issueId,
      @Valid @RequestBody CommentCreateRequest request) {
    CommentResponse commentResponse =
        commentService.createComment(id, issueId, caller.getId(), request);

    return ResponseEntity.created(
            URI.create(
                "/api/projects/" + id + "/issues/" + issueId + "/comments/" + commentResponse.id()))
        .body(ApiResponse.ok(commentResponse));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getComments(
      @PathVariable Integer id,
      @PathVariable Integer issueId,
      @RequestParam(required = false) Integer userId,
      Pageable pageable) {
    PagedResponse<CommentResponse> commentResponse =
        commentService.getCommentsByIssueId(id, issueId, userId, pageable);

    return ResponseEntity.ok(ApiResponse.ok(commentResponse));
  }

  @GetMapping("/{commentId}")
  public ResponseEntity<ApiResponse<CommentResponse>> getCommentById(
      @PathVariable Integer id, @PathVariable Integer issueId, @PathVariable Integer commentId) {
    CommentResponse commentResponse = commentService.getCommentById(id, issueId, commentId);

    return ResponseEntity.ok(ApiResponse.ok(commentResponse));
  }

  /** Author-only, enforced in the service - see {@code CommentService}. */
  @PutMapping("/{commentId}")
  public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @PathVariable Integer issueId,
      @PathVariable Integer commentId,
      @Valid @RequestBody CommentUpdateRequest request) {
    CommentResponse commentResponse =
        commentService.updateComment(id, issueId, commentId, request, caller);

    return ResponseEntity.ok(ApiResponse.ok(commentResponse));
  }

  /** The author, an editor, or the project's leader. */
  @DeleteMapping("/{commentId}")
  public ResponseEntity<ApiResponse<Void>> deleteComment(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @PathVariable Integer issueId,
      @PathVariable Integer commentId) {
    commentService.deleteComment(id, issueId, commentId, caller);

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
