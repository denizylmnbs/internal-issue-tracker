package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.shared.port.FieldCodeUsageResolver;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Owns {@link FieldKind#PROJECT_STATUS} - global, so {@code projectId} is ignored throughout,
 * matching every other port here. */
@Component
@RequiredArgsConstructor
class ProjectFieldCodeUsageAdapter implements FieldCodeUsageResolver {

  private final ProjectRepository repository;

  @Override
  public boolean supports(FieldKind kind) {
    return kind == FieldKind.PROJECT_STATUS;
  }

  @Override
  @Transactional(readOnly = true)
  public long countUsages(FieldKind kind, Integer projectId, String code) {
    return repository.countByStatusAndIsActiveTrue(code);
  }

  @Override
  @Transactional
  public void reassign(FieldKind kind, Integer projectId, String fromCode, String toCode) {
    List<Project> rows = repository.findByStatusAndIsActiveTrue(fromCode);
    rows.forEach(project -> project.setStatus(toCode));
    repository.saveAll(rows);
  }
}
