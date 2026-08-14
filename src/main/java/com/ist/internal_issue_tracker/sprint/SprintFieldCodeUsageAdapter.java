package com.ist.internal_issue_tracker.sprint;

import com.ist.internal_issue_tracker.shared.port.FieldCodeUsageResolver;
import com.ist.internal_issue_tracker.shared.port.FieldDefinitionLookup;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import com.ist.internal_issue_tracker.shared.port.FieldSemantic;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Owns {@link FieldKind#SPRINT_STATUS} - the only code a sprint carries. */
@Component
@RequiredArgsConstructor
class SprintFieldCodeUsageAdapter implements FieldCodeUsageResolver {

  private final SprintRepository repository;
  private final FieldDefinitionLookup fieldDefinitionLookup;

  @Override
  public boolean supports(FieldKind kind) {
    return kind == FieldKind.SPRINT_STATUS;
  }

  @Override
  @Transactional(readOnly = true)
  public long countUsages(FieldKind kind, Integer projectId, String code) {
    return repository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, code);
  }

  /** Also recomputes {@code isRunning}, mirroring {@code SprintService#isRunningStatus} - a bulk
   * code migration must keep it in the same lockstep a normal status change would. */
  @Override
  @Transactional
  public void reassign(FieldKind kind, Integer projectId, String fromCode, String toCode) {
    List<Sprint> rows = repository.findByProjectIdAndStatusAndDeletedAtIsNull(projectId, fromCode);
    boolean nextIsRunning =
        fieldDefinitionLookup
            .codesWithSemantic(projectId, FieldKind.SPRINT_STATUS, FieldSemantic.ACTIVE_WORK)
            .contains(toCode);
    rows.forEach(
        sprint -> {
          sprint.setStatus(toCode);
          sprint.setIsRunning(nextIsRunning);
        });
    repository.saveAll(rows);
  }
}
