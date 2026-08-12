package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.project.dto.ChangeLeaderRequest;
import com.ist.internal_issue_tracker.project.dto.ChangeStatusRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectDetailResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectUpdateRequest;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
  private final ProjectService projectService;

  @PostMapping
  public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @Valid @RequestBody ProjectCreateRequest request) {

    ProjectResponse projectResponse = projectService.createProject(caller.getId(), request);

    return ResponseEntity.created(URI.create("/api/projects/" + projectResponse.id()))
        .body(ApiResponse.ok(projectResponse));
  }

  /** Carries the member and team counts; the list endpoint deliberately does not. */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectById(
      @PathVariable Integer id) {
    ProjectDetailResponse projectResponse = projectService.getProjectById(id);

    return ResponseEntity.ok(ApiResponse.ok(projectResponse));
  }

  /**
   * Covers "all projects", "projects matching a name", "projects in a status", "projects led by
   * someone" and "projects running in a window" - every filter is optional, so an unfiltered call
   * lists everything.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<ProjectResponse>>> getAllProjects(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer leaderId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDateAfter,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDateBefore,
      Pageable pageable) {
    PagedResponse<ProjectResponse> projectResponse =
        projectService.getAllProjects(
            name, status, leaderId, startDateAfter, endDateBefore, pageable);

    return ResponseEntity.ok(ApiResponse.ok(projectResponse));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @Valid @RequestBody ProjectUpdateRequest request) {
    ProjectResponse projectResponse = projectService.updateProject(id, caller.getId(), request);

    return ResponseEntity.ok(ApiResponse.ok(projectResponse));
  }

  @PatchMapping("/{id}/leader")
  public ResponseEntity<ApiResponse<ProjectResponse>> changeLeader(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @Valid @RequestBody ChangeLeaderRequest request) {
    ProjectResponse projectResponse = projectService.changeLeader(id, caller.getId(), request);

    return ResponseEntity.ok(ApiResponse.ok(projectResponse));
  }

  /** Leaves the project with no leader; only an editor can act on it until a new one is named. */
  @DeleteMapping("/{id}/leader")
  public ResponseEntity<ApiResponse<ProjectResponse>> removeLeader(
      @AuthenticationPrincipal AuthenticatedUser caller, @PathVariable Integer id) {
    ProjectResponse projectResponse = projectService.removeLeader(id, caller.getId());

    return ResponseEntity.ok(ApiResponse.ok(projectResponse));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<ProjectResponse>> changeStatus(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @Valid @RequestBody ChangeStatusRequest request) {
    ProjectResponse projectResponse = projectService.changeStatus(id, caller.getId(), request);

    return ResponseEntity.ok(ApiResponse.ok(projectResponse));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteProject(
      @AuthenticationPrincipal AuthenticatedUser caller, @PathVariable Integer id) {
    projectService.deleteProject(id, caller.getId());

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
