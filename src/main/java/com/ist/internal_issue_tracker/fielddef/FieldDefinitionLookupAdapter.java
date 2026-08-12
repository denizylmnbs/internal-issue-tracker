package com.ist.internal_issue_tracker.fielddef;

import com.ist.internal_issue_tracker.shared.port.FieldDefinitionLookup;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import com.ist.internal_issue_tracker.shared.port.FieldSemantic;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Not cached, unlike most adapters in {@code shared.port}. Every consuming module calls {@link
 * #isValidCode} on the write path of its own entity (an issue's status, a team's field, ...), and
 * that path must see a field definition the moment it is created - a two-minute-stale cache would
 * mean a status just added by {@code FieldDefinitionService} is rejected by the very next request
 * that tries to use it. {@code IssueMetricsService} is where the hot, cacheable read of this data
 * happens, and it is cached and evicted there instead - see {@code MetricsCacheEvictionListener}.
 */
@Component
@RequiredArgsConstructor
public class FieldDefinitionLookupAdapter implements FieldDefinitionLookup {

  private final FieldDefinitionRepository repository;

  private Integer scope(Integer projectId, FieldKind kind) {
    return kind.isGlobal() ? null : projectId;
  }

  @Override
  public boolean isValidCode(Integer projectId, FieldKind kind, String code) {
    if (code == null) {
      return false;
    }
    Integer scoped = scope(projectId, kind);
    return (scoped != null
            ? repository.findByProjectIdAndKindAndCodeAndIsActiveTrue(scoped, kind, code)
            : repository.findByProjectIdIsNullAndKindAndCodeAndIsActiveTrue(kind, code))
        .isPresent();
  }

  @Override
  public String defaultCode(Integer projectId, FieldKind kind) {
    Integer scoped = scope(projectId, kind);
    return (scoped != null
            ? repository.findByProjectIdAndKindAndIsDefaultTrueAndIsActiveTrue(scoped, kind)
            : repository.findByProjectIdIsNullAndKindAndIsDefaultTrueAndIsActiveTrue(kind))
        .map(FieldDefinition::getCode)
        .orElse(null);
  }

  @Override
  public Set<String> codesWithSemantic(Integer projectId, FieldKind kind, FieldSemantic semantic) {
    Integer scoped = scope(projectId, kind);
    if (scoped == null) {
      // Global kinds carry no metric semantics today - nothing consumes this combination.
      return Set.of();
    }

    List<FieldDefinition> rows =
        switch (semantic) {
          case DONE -> repository.findByProjectIdAndKindAndIsDoneTrueAndIsActiveTrue(scoped, kind);
          case CANCELLED ->
              repository.findByProjectIdAndKindAndIsCancelledTrueAndIsActiveTrue(scoped, kind);
          case ACTIVE_WORK ->
              repository.findByProjectIdAndKindAndIsActiveWorkTrueAndIsActiveTrue(scoped, kind);
          case DEFECT ->
              repository.findByProjectIdAndKindAndIsDefectTrueAndIsActiveTrue(scoped, kind);
        };

    return rows.stream().map(FieldDefinition::getCode).collect(Collectors.toUnmodifiableSet());
  }
}
