package com.ist.internal_issue_tracker.activity;

import com.ist.internal_issue_tracker.activity.dto.ActivityResponse;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * History, addressed under the project that owns it. The path variable holding the project is named
 * {@code id} for the reason given on {@code SprintController} - {@code SecurityConfig} reads it
 * literally.
 *
 * <p>Read-only by design and not merely by omission: an activity row is written by a listener or not
 * at all, so there is no POST, PUT or DELETE to leave out.
 *
 * <p>These are the first routes in the codebase where reading is restricted beyond being logged in.
 * Everything else falls through to {@code anyRequest().authenticated()}, which is defensible for a
 * board; a feed of who changed what and when is a different kind of thing, so it is held to the same
 * participation rule as writing.
 */
@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class ActivityController {

  private final ActivityService activityService;

  @GetMapping("/issues/{issueId}/activities")
  public ResponseEntity<ApiResponse<PagedResponse<ActivityResponse>>> getIssueActivities(
      @PathVariable Integer id, @PathVariable Integer issueId, Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.ok(activityService.getIssueActivities(id, issueId, pageable)));
  }

  @GetMapping("/sprints/{sprintId}/activities")
  public ResponseEntity<ApiResponse<PagedResponse<ActivityResponse>>> getSprintActivities(
      @PathVariable Integer id, @PathVariable Integer sprintId, Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.ok(activityService.getSprintActivities(id, sprintId, pageable)));
  }

  /** The project's own history - see {@code ActivityService#getProjectActivities}. */
  @GetMapping("/activities")
  public ResponseEntity<ApiResponse<PagedResponse<ActivityResponse>>> getProjectActivities(
      @PathVariable Integer id, Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(activityService.getProjectActivities(id, pageable)));
  }
}
