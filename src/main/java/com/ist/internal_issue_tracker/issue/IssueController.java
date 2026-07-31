package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.issue.dto.ChangeAssigneeRequest;
import com.ist.internal_issue_tracker.issue.dto.ChangeStatusRequest;
import com.ist.internal_issue_tracker.issue.dto.IssueCreateRequest;
import com.ist.internal_issue_tracker.issue.dto.IssueResponse;
import com.ist.internal_issue_tracker.issue.dto.IssueUpdateRequest;
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
 * Issues live under their project, and the path variable holding the project is named {@code id}
 * rather than {@code projectId} for the reason given on {@code SprintController}.
 *
 * <p>These are the first routes open to project participants rather than only editors and leaders,
 * which is what {@code SecurityConfig}'s {@code editorLeaderOrParticipant} is for. Deleting an issue
 * stays with editors and leaders.
 */
@RestController
@RequestMapping("/api/projects/{id}/issues")
@RequiredArgsConstructor
public class IssueController {

  private final IssueService issueService;

  /** The reporter is the caller, so it comes from the principal and never from the body. */
  @PostMapping
  public ResponseEntity<ApiResponse<IssueResponse>> createIssue(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @Valid @RequestBody IssueCreateRequest request) {
    IssueResponse issueResponse = issueService.createIssue(id, caller.getId(), request);

    return ResponseEntity.created(
            URI.create("/api/projects/" + id + "/issues/" + issueResponse.id()))
        .body(ApiResponse.ok(issueResponse));
  }

  /**
   * Every filter is optional. Together they cover the questions a board asks - what is in this
   * sprint, what is assigned to me, what is still open and critical - without an endpoint each.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<IssueResponse>>> getIssues(
      @PathVariable Integer id,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) IssueType type,
      @RequestParam(required = false) IssueStatus status,
      @RequestParam(required = false) IssuePriority priority,
      @RequestParam(required = false) Integer sprintId,
      @RequestParam(required = false) Integer epicId,
      @RequestParam(required = false) Integer reporterId,
      @RequestParam(required = false) Integer assigneeUserId,
      @RequestParam(required = false) Integer assigneeTeamId,
      Pageable pageable) {
    PagedResponse<IssueResponse> issueResponse =
        issueService.getIssuesByProjectId(
            id,
            name,
            type,
            status,
            priority,
            sprintId,
            epicId,
            reporterId,
            assigneeUserId,
            assigneeTeamId,
            pageable);

    return ResponseEntity.ok(ApiResponse.ok(issueResponse));
  }

  @GetMapping("/{issueId}")
  public ResponseEntity<ApiResponse<IssueResponse>> getIssueById(
      @PathVariable Integer id, @PathVariable Integer issueId) {
    IssueResponse issueResponse = issueService.getIssueById(id, issueId);

    return ResponseEntity.ok(ApiResponse.ok(issueResponse));
  }

  @PutMapping("/{issueId}")
  public ResponseEntity<ApiResponse<IssueResponse>> updateIssue(
      @PathVariable Integer id,
      @PathVariable Integer issueId,
      @Valid @RequestBody IssueUpdateRequest request) {
    IssueResponse issueResponse = issueService.updateIssue(id, issueId, request);

    return ResponseEntity.ok(ApiResponse.ok(issueResponse));
  }

  @PatchMapping("/{issueId}/status")
  public ResponseEntity<ApiResponse<IssueResponse>> changeStatus(
      @PathVariable Integer id,
      @PathVariable Integer issueId,
      @Valid @RequestBody ChangeStatusRequest request) {
    IssueResponse issueResponse = issueService.changeStatus(id, issueId, request);

    return ResponseEntity.ok(ApiResponse.ok(issueResponse));
  }

  @PatchMapping("/{issueId}/assignee")
  public ResponseEntity<ApiResponse<IssueResponse>> changeAssignee(
      @PathVariable Integer id,
      @PathVariable Integer issueId,
      @Valid @RequestBody ChangeAssigneeRequest request) {
    IssueResponse issueResponse = issueService.changeAssignee(id, issueId, request);

    return ResponseEntity.ok(ApiResponse.ok(issueResponse));
  }

  /** Leaves the issue with no assignee at all - see {@code ProjectController#removeLeader}. */
  @DeleteMapping("/{issueId}/assignee")
  public ResponseEntity<ApiResponse<IssueResponse>> removeAssignee(
      @PathVariable Integer id, @PathVariable Integer issueId) {
    IssueResponse issueResponse = issueService.removeAssignee(id, issueId);

    return ResponseEntity.ok(ApiResponse.ok(issueResponse));
  }

  @DeleteMapping("/{issueId}")
  public ResponseEntity<ApiResponse<Void>> deleteIssue(
      @PathVariable Integer id, @PathVariable Integer issueId) {
    issueService.deleteIssue(id, issueId);

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
