package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.project.dto.ProjectMemberCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectMemberResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectParticipantResponse;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Membership is addressed under the project that owns it. The path variable is named {@code id}
 * rather than {@code projectId} on purpose: {@code SecurityConfig}'s authorization managers read it
 * through {@code RequestAuthorizationContext#getVariables()}, which looks the name up literally.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectMemberController {
  private final ProjectMemberService projectMemberService;

  @PostMapping("/{id}/members")
  public ResponseEntity<ApiResponse<ProjectMemberResponse>> createProjectMember(
      @PathVariable Integer id, @Valid @RequestBody ProjectMemberCreateRequest request) {
    ProjectMemberResponse response = projectMemberService.createProjectMember(id, request);

    return ResponseEntity.created(URI.create("/api/projects/" + id + "/members/" + response.userId()))
        .body(ApiResponse.ok(response));
  }

  @GetMapping("/{id}/members")
  public ResponseEntity<ApiResponse<PagedResponse<ProjectMemberResponse>>> getProjectMembers(
      @PathVariable Integer id, Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(projectMemberService.getProjectMembers(id, pageable)));
  }

  /**
   * Everyone who works on the project, including people reached through an assigned team. This is
   * the list that matches {@code memberCount} on the project detail; {@code /members} above is the
   * narrower set of direct assignments that POST and DELETE act on.
   */
  @GetMapping("/{id}/participants")
  public ResponseEntity<ApiResponse<PagedResponse<ProjectParticipantResponse>>>
      getProjectParticipants(@PathVariable Integer id, Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.ok(projectMemberService.getProjectParticipants(id, pageable)));
  }

  /** Soft delete, keyed by the pair {@code unique_active_project_user} keys a live assignment on. */
  @DeleteMapping("/{id}/members/{userId}")
  public ResponseEntity<ApiResponse<Void>> removeProjectMember(
      @PathVariable Integer id, @PathVariable Integer userId) {
    projectMemberService.removeProjectMember(id, userId);

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
