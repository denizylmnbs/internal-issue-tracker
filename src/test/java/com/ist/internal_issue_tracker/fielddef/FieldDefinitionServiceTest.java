package com.ist.internal_issue_tracker.fielddef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ist.internal_issue_tracker.fielddef.dto.FieldDefinitionCreateRequest;
import com.ist.internal_issue_tracker.fielddef.dto.ReorderRequest;
import com.ist.internal_issue_tracker.fielddef.exception.FieldDefErrorCode;
import com.ist.internal_issue_tracker.fielddef.exception.FieldDefinitionRuleViolationException;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Guards the invariants that {@code IssueMetricsRepository} leans on now that {@code ISSUE_STATUS}
 * is per-project data rather than a fixed enum: a project can never be left with zero active
 * "done" statuses or with no default. This is the invariant {@code MetricStatusCoverageTest} used
 * to enforce by comparing {@code MetricStatus} against the global {@code IssueStatus} enum - there
 * is no such global enum to compare against anymore, so the guarantee moves from a compile-adjacent
 * coverage test to a runtime check in the service that owns the data.
 */
@ExtendWith(MockitoExtension.class)
class FieldDefinitionServiceTest {

  @Mock private FieldDefinitionRepository repository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private FieldDefinitionService service;

  private static FieldDefinition definition(
      Integer id, FieldKind kind, Integer projectId, boolean isDefault, boolean isDone) {
    FieldDefinition field = new FieldDefinition();
    field.setId(id);
    field.setKind(kind);
    field.setProjectId(projectId);
    field.setCode("CODE_" + id);
    field.setLabel("Label " + id);
    field.setSortOrder(0);
    field.setIsActive(true);
    field.setIsDefault(isDefault);
    field.setIsDone(isDone);
    return field;
  }

  @Test
  void delete_throws_whenItIsTheOnlyDoneStatusForAProject() {
    FieldDefinition onlyDone = definition(1, FieldKind.ISSUE_STATUS, 5, false, true);
    when(repository.findByIdAndProjectId(1, 5)).thenReturn(Optional.of(onlyDone));
    when(repository.countByProjectIdAndKindAndIsDoneTrueAndIsActiveTrue(5, FieldKind.ISSUE_STATUS))
        .thenReturn(1L);

    assertThatThrownBy(() -> service.delete(5, 1))
        .isInstanceOf(FieldDefinitionRuleViolationException.class)
        .extracting(e -> ((AppException) e).errorCode())
        .isEqualTo(FieldDefErrorCode.LAST_DONE_FIELD_REQUIRED);
  }

  @Test
  void delete_succeeds_whenAnotherDoneStatusExists() {
    FieldDefinition done = definition(1, FieldKind.ISSUE_STATUS, 5, false, true);
    when(repository.findByIdAndProjectId(1, 5)).thenReturn(Optional.of(done));
    when(repository.countByProjectIdAndKindAndIsDoneTrueAndIsActiveTrue(5, FieldKind.ISSUE_STATUS))
        .thenReturn(2L);
    when(repository.save(any())).thenReturn(done);

    service.delete(5, 1);

    assertThat(done.getIsActive()).isFalse();
  }

  @Test
  void delete_throws_whenItIsTheOnlyDefault() {
    FieldDefinition onlyDefault = definition(2, FieldKind.ISSUE_TYPE, 5, true, false);
    when(repository.findByIdAndProjectId(2, 5)).thenReturn(Optional.of(onlyDefault));
    when(repository.countByProjectIdAndKindAndIsDefaultTrueAndIsActiveTrue(5, FieldKind.ISSUE_TYPE))
        .thenReturn(1L);

    assertThatThrownBy(() -> service.delete(5, 2))
        .isInstanceOf(FieldDefinitionRuleViolationException.class)
        .extracting(e -> ((AppException) e).errorCode())
        .isEqualTo(FieldDefErrorCode.DEFAULT_FIELD_REQUIRED);
  }

  @Test
  void create_throws_whenAProjectScopedKindIsGivenNoProject() {
    FieldDefinitionCreateRequest request =
        new FieldDefinitionCreateRequest(
            FieldKind.ISSUE_STATUS, "SHIPPED", "Shipped", null, false, true, false, false, false);

    assertThatThrownBy(() -> service.create(null, request))
        .isInstanceOf(FieldDefinitionRuleViolationException.class)
        .extracting(e -> ((AppException) e).errorCode())
        .isEqualTo(FieldDefErrorCode.FIELD_KIND_NOT_GLOBAL);
  }

  @Test
  void create_throws_whenAGlobalKindIsGivenAProject() {
    FieldDefinitionCreateRequest request =
        new FieldDefinitionCreateRequest(
            FieldKind.TEAM_FIELD, "SECURITY", "Security", null, false, false, false, false, false);

    assertThatThrownBy(() -> service.create(5, request))
        .isInstanceOf(FieldDefinitionRuleViolationException.class)
        .extracting(e -> ((AppException) e).errorCode())
        .isEqualTo(FieldDefErrorCode.FIELD_KIND_NOT_PROJECT_SCOPED);
  }

  @Test
  void reorder_throws_whenTheIdSetDoesNotMatchTheProjectsActiveRows() {
    FieldDefinition existing = definition(1, FieldKind.ISSUE_STATUS, 5, true, false);
    when(repository.findByProjectIdAndKindAndIsActiveTrueOrderBySortOrder(5, FieldKind.ISSUE_STATUS))
        .thenReturn(List.of(existing));

    ReorderRequest request = new ReorderRequest(FieldKind.ISSUE_STATUS, List.of(1, 2));

    assertThatThrownBy(() -> service.reorder(5, request))
        .isInstanceOf(FieldDefinitionRuleViolationException.class)
        .extracting(e -> ((AppException) e).errorCode())
        .isEqualTo(FieldDefErrorCode.REORDER_SET_MISMATCH);
  }
}
