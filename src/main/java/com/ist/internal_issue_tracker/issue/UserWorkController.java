package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.issue.dto.IssueResponse;
import com.ist.internal_issue_tracker.issue.dto.UserSprintProgressResponse;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mounted under {@code /api/users}, same as {@code UserProjectsController} and {@code
 * UserTeamsController} - the URL says nothing about which module owns the handler, and this data
 * belongs to {@code issue} because both queries it answers cut across every project the user
 * touches, which no project-scoped controller can do.
 *
 * <p>Path variable is named {@code id}, not {@code userId}, so {@code SecurityConfig}'s {@code
 * selfOrAdmin} rule - which reads the literal path variable {@code "id"} - applies to both routes.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserWorkController {
  private final UserWorkService userWorkService;

  @GetMapping("/{id}/issues")
  public ResponseEntity<ApiResponse<PagedResponse<IssueResponse>>> getActiveIssuesByUserId(
      @PathVariable Integer id, Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(userWorkService.getActiveIssuesByUserId(id, pageable)));
  }

  @GetMapping("/{id}/sprint-progress")
  public ResponseEntity<ApiResponse<UserSprintProgressResponse>> getSprintProgress(
      @PathVariable Integer id) {
    return ResponseEntity.ok(ApiResponse.ok(userWorkService.getSprintProgress(id)));
  }
}
