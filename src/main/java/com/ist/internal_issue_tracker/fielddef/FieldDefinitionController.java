package com.ist.internal_issue_tracker.fielddef;

import com.ist.internal_issue_tracker.fielddef.dto.FieldDefinitionCreateRequest;
import com.ist.internal_issue_tracker.fielddef.dto.FieldDefinitionResponse;
import com.ist.internal_issue_tracker.fielddef.dto.FieldDefinitionUpdateRequest;
import com.ist.internal_issue_tracker.fielddef.dto.ReorderRequest;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The project-scoped six kinds: {@code SPRINT_STATUS}, {@code EPIC_STATUS}, {@code ISSUE_STATUS},
 * {@code ISSUE_TYPE}, {@code ISSUE_PRIORITY}, {@code ISSUE_UNIT}. {@code PROJECT_STATUS} and
 * {@code TEAM_FIELD} are not reachable here - see {@link GlobalFieldDefinitionController}.
 *
 * <p>The path variable is named {@code id}, matching {@code EpicController}, so the existing
 * {@code editorOrProjectLeader} authorization rule covers these routes without a new one.
 */
@RestController
@RequestMapping("/api/projects/{id}/field-definitions")
@RequiredArgsConstructor
public class FieldDefinitionController {

  private final FieldDefinitionService fieldDefinitionService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<FieldDefinitionResponse>>> getFieldDefinitions(
      @PathVariable Integer id, @RequestParam(required = false) FieldKind kind) {
    List<FieldDefinitionResponse> response = fieldDefinitionService.list(id, kind);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<FieldDefinitionResponse>> createFieldDefinition(
      @PathVariable Integer id, @Valid @RequestBody FieldDefinitionCreateRequest request) {
    FieldDefinitionResponse response = fieldDefinitionService.create(id, request);

    return ResponseEntity.created(
            URI.create("/api/projects/" + id + "/field-definitions/" + response.id()))
        .body(ApiResponse.ok(response));
  }

  @PutMapping("/{defId}")
  public ResponseEntity<ApiResponse<FieldDefinitionResponse>> updateFieldDefinition(
      @PathVariable Integer id,
      @PathVariable Integer defId,
      @Valid @RequestBody FieldDefinitionUpdateRequest request) {
    FieldDefinitionResponse response = fieldDefinitionService.update(id, defId, request);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PatchMapping("/reorder")
  public ResponseEntity<ApiResponse<List<FieldDefinitionResponse>>> reorderFieldDefinitions(
      @PathVariable Integer id, @Valid @RequestBody ReorderRequest request) {
    List<FieldDefinitionResponse> response = fieldDefinitionService.reorder(id, request);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @DeleteMapping("/{defId}")
  public ResponseEntity<ApiResponse<Void>> deleteFieldDefinition(
      @PathVariable Integer id, @PathVariable Integer defId) {
    fieldDefinitionService.delete(id, defId);

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
