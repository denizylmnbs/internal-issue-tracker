package com.ist.internal_issue_tracker.epic;

import com.ist.internal_issue_tracker.shared.port.FieldCodeUsageResolver;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Owns {@link FieldKind#EPIC_STATUS} - the only code an epic carries. */
@Component
@RequiredArgsConstructor
class EpicFieldCodeUsageAdapter implements FieldCodeUsageResolver {

  private final EpicRepository repository;

  @Override
  public boolean supports(FieldKind kind) {
    return kind == FieldKind.EPIC_STATUS;
  }

  @Override
  @Transactional(readOnly = true)
  public long countUsages(FieldKind kind, Integer projectId, String code) {
    return repository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, code);
  }

  @Override
  @Transactional
  public void reassign(FieldKind kind, Integer projectId, String fromCode, String toCode) {
    List<Epic> rows = repository.findByProjectIdAndStatusAndDeletedAtIsNull(projectId, fromCode);
    rows.forEach(epic -> epic.setStatus(toCode));
    repository.saveAll(rows);
  }
}
