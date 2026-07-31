package com.ist.internal_issue_tracker.epic;

import com.ist.internal_issue_tracker.epic.dto.ChangeStatusRequest;
import com.ist.internal_issue_tracker.epic.dto.EpicCreateRequest;
import com.ist.internal_issue_tracker.epic.dto.EpicResponse;
import com.ist.internal_issue_tracker.epic.dto.EpicUpdateRequest;
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
 * Epics live under their project, and the path variable holding the project is named {@code id}
 * rather than {@code projectId} on purpose - see {@code SprintController} for why that naming is
 * what lets the existing {@code editorOrProjectLeader} rule cover these routes.
 */
@RestController
@RequestMapping("/api/projects/{id}/epics")
@RequiredArgsConstructor
public class EpicController {

  private final EpicService epicService;

  /** The reporter is the caller, so it comes from the principal and never from the body. */
  @PostMapping
  public ResponseEntity<ApiResponse<EpicResponse>> createEpic(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @Valid @RequestBody EpicCreateRequest request) {
    EpicResponse epicResponse = epicService.createEpic(id, caller.getId(), request);

    return ResponseEntity.created(URI.create("/api/projects/" + id + "/epics/" + epicResponse.id()))
        .body(ApiResponse.ok(epicResponse));
  }

  /** Every filter is optional, so an unfiltered call lists the project's whole epic history. */
  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<EpicResponse>>> getEpics(
      @PathVariable Integer id,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) EpicStatus status,
      @RequestParam(required = false) Integer reporterId,
      Pageable pageable) {
    PagedResponse<EpicResponse> epicResponse =
        epicService.getEpicsByProjectId(id, name, status, reporterId, pageable);

    return ResponseEntity.ok(ApiResponse.ok(epicResponse));
  }

  @GetMapping("/{epicId}")
  public ResponseEntity<ApiResponse<EpicResponse>> getEpicById(
      @PathVariable Integer id, @PathVariable Integer epicId) {
    EpicResponse epicResponse = epicService.getEpicById(id, epicId);

    return ResponseEntity.ok(ApiResponse.ok(epicResponse));
  }

  @PutMapping("/{epicId}")
  public ResponseEntity<ApiResponse<EpicResponse>> updateEpic(
      @PathVariable Integer id,
      @PathVariable Integer epicId,
      @Valid @RequestBody EpicUpdateRequest request) {
    EpicResponse epicResponse = epicService.updateEpic(id, epicId, request);

    return ResponseEntity.ok(ApiResponse.ok(epicResponse));
  }

  @PatchMapping("/{epicId}/status")
  public ResponseEntity<ApiResponse<EpicResponse>> changeStatus(
      @PathVariable Integer id,
      @PathVariable Integer epicId,
      @Valid @RequestBody ChangeStatusRequest request) {
    EpicResponse epicResponse = epicService.changeStatus(id, epicId, request);

    return ResponseEntity.ok(ApiResponse.ok(epicResponse));
  }

  @DeleteMapping("/{epicId}")
  public ResponseEntity<ApiResponse<Void>> deleteEpic(
      @PathVariable Integer id, @PathVariable Integer epicId) {
    epicService.deleteEpic(id, epicId);

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
