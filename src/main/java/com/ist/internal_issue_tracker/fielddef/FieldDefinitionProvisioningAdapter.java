package com.ist.internal_issue_tracker.fielddef;

import com.ist.internal_issue_tracker.shared.port.FieldDefinitionProvisioning;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Copies the six project-scoped kinds' global template rows ({@code project_id IS NULL}) into a
 * fresh project's own rows. {@code PROJECT_STATUS} and {@code TEAM_FIELD} are skipped - they stay
 * global, there is nothing project-scoped to seed.
 */
@Component
@RequiredArgsConstructor
class FieldDefinitionProvisioningAdapter implements FieldDefinitionProvisioning {

  private static final List<FieldKind> PROJECT_SCOPED_KINDS =
      List.of(
          FieldKind.SPRINT_STATUS,
          FieldKind.EPIC_STATUS,
          FieldKind.ISSUE_STATUS,
          FieldKind.ISSUE_TYPE,
          FieldKind.ISSUE_PRIORITY,
          FieldKind.ISSUE_UNIT);

  private final FieldDefinitionRepository repository;

  @Override
  public void seedDefaults(Integer projectId) {
    for (FieldKind kind : PROJECT_SCOPED_KINDS) {
      List<FieldDefinition> templates =
          repository.findByProjectIdIsNullAndKindAndIsActiveTrueOrderBySortOrder(kind);

      List<FieldDefinition> copies = templates.stream().map(t -> copyOnto(t, projectId)).toList();
      repository.saveAll(copies);
    }
  }

  private static FieldDefinition copyOnto(FieldDefinition template, Integer projectId) {
    FieldDefinition copy = new FieldDefinition();
    copy.setKind(template.getKind());
    copy.setProjectId(projectId);
    copy.setCode(template.getCode());
    copy.setLabel(template.getLabel());
    copy.setColor(template.getColor());
    copy.setSortOrder(template.getSortOrder());
    copy.setIsDefault(template.getIsDefault());
    copy.setIsDone(template.getIsDone());
    copy.setIsCancelled(template.getIsCancelled());
    copy.setIsActiveWork(template.getIsActiveWork());
    copy.setIsDefect(template.getIsDefect());
    return copy;
  }
}
