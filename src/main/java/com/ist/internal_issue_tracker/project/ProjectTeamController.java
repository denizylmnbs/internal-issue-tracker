package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.project.dto.ProjectTeamCreateRequest;
import com.ist.internal_issue_tracker.project.dto.ProjectTeamResponse;
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

/** Team assignments, addressed under the project - see {@link ProjectMemberController}. */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectTeamController {
  private final ProjectTeamService projectTeamService;

  @PostMapping("/{id}/teams")
  public ResponseEntity<ApiResponse<ProjectTeamResponse>> createProjectTeam(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @Valid @RequestBody ProjectTeamCreateRequest request) {
    ProjectTeamResponse response =
        projectTeamService.createProjectTeam(id, caller.getId(), request);

    return ResponseEntity.created(URI.create("/api/projects/" + id + "/teams/" + response.teamId()))
        .body(ApiResponse.ok(response));
  }

  @GetMapping("/{id}/teams")
  public ResponseEntity<ApiResponse<PagedResponse<ProjectTeamResponse>>> getProjectTeams(
      @PathVariable Integer id, Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(projectTeamService.getProjectTeams(id, pageable)));
  }

  @DeleteMapping("/{id}/teams/{teamId}")
  public ResponseEntity<ApiResponse<Void>> removeProjectTeam(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @PathVariable Integer teamId) {
    projectTeamService.removeProjectTeam(id, teamId, caller.getId());

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
