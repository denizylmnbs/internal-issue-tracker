package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.shared.port.FieldCodeUsageResolver;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Owns the four kinds an issue carries a code from: {@code type}, {@code resolvingUnit}, {@code
 * status}, {@code priority}. */
@Component
@RequiredArgsConstructor
class IssueFieldCodeUsageAdapter implements FieldCodeUsageResolver {

  private static final Set<FieldKind> SUPPORTED =
      Set.of(
          FieldKind.ISSUE_STATUS,
          FieldKind.ISSUE_TYPE,
          FieldKind.ISSUE_PRIORITY,
          FieldKind.ISSUE_UNIT);

  private final IssueRepository repository;

  @Override
  public boolean supports(FieldKind kind) {
    return SUPPORTED.contains(kind);
  }

  @Override
  @Transactional(readOnly = true)
  public long countUsages(FieldKind kind, Integer projectId, String code) {
    return switch (kind) {
      case ISSUE_STATUS -> repository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, code);
      case ISSUE_TYPE -> repository.countByProjectIdAndTypeAndDeletedAtIsNull(projectId, code);
      case ISSUE_PRIORITY ->
          repository.countByProjectIdAndPriorityAndDeletedAtIsNull(projectId, code);
      case ISSUE_UNIT ->
          repository.countByProjectIdAndResolvingUnitAndDeletedAtIsNull(projectId, code);
      default -> throw new IllegalArgumentException("Unsupported kind: " + kind);
    };
  }

  @Override
  @Transactional
  public void reassign(FieldKind kind, Integer projectId, String fromCode, String toCode) {
    List<Issue> rows =
        switch (kind) {
          case ISSUE_STATUS ->
              repository.findByProjectIdAndStatusAndDeletedAtIsNull(projectId, fromCode);
          case ISSUE_TYPE -> repository.findByProjectIdAndTypeAndDeletedAtIsNull(projectId, fromCode);
          case ISSUE_PRIORITY ->
              repository.findByProjectIdAndPriorityAndDeletedAtIsNull(projectId, fromCode);
          case ISSUE_UNIT ->
              repository.findByProjectIdAndResolvingUnitAndDeletedAtIsNull(projectId, fromCode);
          default -> throw new IllegalArgumentException("Unsupported kind: " + kind);
        };

    for (Issue issue : rows) {
      switch (kind) {
        case ISSUE_STATUS -> issue.setStatus(toCode);
        case ISSUE_TYPE -> issue.setType(toCode);
        case ISSUE_PRIORITY -> issue.setPriority(toCode);
        case ISSUE_UNIT -> issue.setResolvingUnit(toCode);
        default -> throw new IllegalArgumentException("Unsupported kind: " + kind);
      }
    }
    repository.saveAll(rows);
  }
}
