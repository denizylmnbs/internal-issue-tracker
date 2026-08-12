package com.ist.internal_issue_tracker.sprint;

import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.sprint.dto.ChangeStatusRequest;
import com.ist.internal_issue_tracker.sprint.dto.SprintCreateRequest;
import com.ist.internal_issue_tracker.sprint.dto.SprintResponse;
import com.ist.internal_issue_tracker.sprint.dto.SprintUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Sprints live under their project, and the path variable holding the project is named {@code id}
 * rather than {@code projectId} on purpose: {@code SecurityConfig}'s leader check reads the literal
 * {@code "id"} variable, so this naming is what lets the existing {@code editorOrProjectLeader}
 * rule cover these routes without a sprint-shaped port of its own.
 */
@RestController
@RequestMapping("/api/projects/{id}/sprints")
@RequiredArgsConstructor
public class SprintController {

  private final SprintService sprintService;

  @PostMapping
  public ResponseEntity<ApiResponse<SprintResponse>> createSprint(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @Valid @RequestBody SprintCreateRequest request) {
    SprintResponse sprintResponse = sprintService.createSprint(id, caller.getId(), request);

    return ResponseEntity.created(
            URI.create("/api/projects/" + id + "/sprints/" + sprintResponse.id()))
        .body(ApiResponse.ok(sprintResponse));
  }

  /** Every filter is optional, so an unfiltered call lists the project's whole sprint history. */
  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<SprintResponse>>> getSprints(
      @PathVariable Integer id,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String status,
      Pageable pageable) {
    PagedResponse<SprintResponse> sprintResponse =
        sprintService.getSprintsByProjectId(id, name, status, pageable);

    return ResponseEntity.ok(ApiResponse.ok(sprintResponse));
  }

  @GetMapping("/{sprintId}")
  public ResponseEntity<ApiResponse<SprintResponse>> getSprintById(
      @PathVariable Integer id, @PathVariable Integer sprintId) {
    SprintResponse sprintResponse = sprintService.getSprintById(id, sprintId);

    return ResponseEntity.ok(ApiResponse.ok(sprintResponse));
  }

  @PutMapping("/{sprintId}")
  public ResponseEntity<ApiResponse<SprintResponse>> updateSprint(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @PathVariable Integer sprintId,
      @Valid @RequestBody SprintUpdateRequest request) {
    SprintResponse sprintResponse =
        sprintService.updateSprint(id, sprintId, caller.getId(), request);

    return ResponseEntity.ok(ApiResponse.ok(sprintResponse));
  }

  @PatchMapping("/{sprintId}/status")
  public ResponseEntity<ApiResponse<SprintResponse>> changeStatus(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @PathVariable Integer sprintId,
      @Valid @RequestBody ChangeStatusRequest request) {
    SprintResponse sprintResponse =
        sprintService.changeStatus(id, sprintId, caller.getId(), request);

    return ResponseEntity.ok(ApiResponse.ok(sprintResponse));
  }

  @DeleteMapping("/{sprintId}")
  public ResponseEntity<ApiResponse<Void>> deleteSprint(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @PathVariable Integer sprintId) {
    sprintService.deleteSprint(id, sprintId, caller.getId());

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
