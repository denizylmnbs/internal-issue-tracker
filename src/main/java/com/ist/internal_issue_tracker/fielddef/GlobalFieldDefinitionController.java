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
 * The two global kinds: {@code PROJECT_STATUS} and {@code TEAM_FIELD}. Writes are {@code ADMIN}-
 * only (see {@code SecurityConfig}) since a change here is instance-wide, not scoped to anyone's
 * own project.
 */
@RestController
@RequestMapping("/api/field-definitions")
@RequiredArgsConstructor
public class GlobalFieldDefinitionController {

  private final FieldDefinitionService fieldDefinitionService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<FieldDefinitionResponse>>> getFieldDefinitions(
      @RequestParam(required = false) FieldKind kind) {
    List<FieldDefinitionResponse> response = fieldDefinitionService.list(null, kind);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<FieldDefinitionResponse>> createFieldDefinition(
      @Valid @RequestBody FieldDefinitionCreateRequest request) {
    FieldDefinitionResponse response = fieldDefinitionService.create(null, request);

    return ResponseEntity.created(URI.create("/api/field-definitions/" + response.id()))
        .body(ApiResponse.ok(response));
  }

  @PutMapping("/{defId}")
  public ResponseEntity<ApiResponse<FieldDefinitionResponse>> updateFieldDefinition(
      @PathVariable Integer defId, @Valid @RequestBody FieldDefinitionUpdateRequest request) {
    FieldDefinitionResponse response = fieldDefinitionService.update(null, defId, request);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PatchMapping("/reorder")
  public ResponseEntity<ApiResponse<List<FieldDefinitionResponse>>> reorderFieldDefinitions(
      @Valid @RequestBody ReorderRequest request) {
    List<FieldDefinitionResponse> response = fieldDefinitionService.reorder(null, request);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @DeleteMapping("/{defId}")
  public ResponseEntity<ApiResponse<Void>> deleteFieldDefinition(@PathVariable Integer defId) {
    fieldDefinitionService.delete(null, defId);

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
