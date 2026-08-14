package com.ist.internal_issue_tracker.fielddef;

import com.ist.internal_issue_tracker.fielddef.dto.FieldDefinitionCreateRequest;
import com.ist.internal_issue_tracker.fielddef.dto.FieldDefinitionResponse;
import com.ist.internal_issue_tracker.fielddef.dto.FieldDefinitionUpdateRequest;
import com.ist.internal_issue_tracker.fielddef.dto.ReorderRequest;
import com.ist.internal_issue_tracker.fielddef.exception.FieldCodeAlreadyExistsException;
import com.ist.internal_issue_tracker.fielddef.exception.FieldDefErrorCode;
import com.ist.internal_issue_tracker.fielddef.exception.FieldDefinitionNotFoundException;
import com.ist.internal_issue_tracker.fielddef.exception.FieldDefinitionRuleViolationException;
import com.ist.internal_issue_tracker.shared.event.FieldDefinitionsChangedEvent;
import com.ist.internal_issue_tracker.shared.port.FieldCodeUsageResolver;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD and the invariants that keep the metrics honest: every project-scoped kind must always have
 * exactly one active default and, for {@link FieldKind#ISSUE_STATUS}, at least one active done row
 * - see {@code IssueMetricsRepository}, which would silently stop finding either otherwise.
 *
 * <p>Global kinds ({@link FieldKind#isGlobal()}) are addressed with {@code projectId = null}
 * throughout; {@link GlobalFieldDefinitionController} and {@link FieldDefinitionController} are
 * the two callers, and each rejects the kinds that do not belong to it before reaching here.
 */
@Service
@RequiredArgsConstructor
public class FieldDefinitionService {

  private final FieldDefinitionRepository repository;
  private final ApplicationEventPublisher eventPublisher;
  private final List<FieldCodeUsageResolver> usageResolvers;

  private FieldCodeUsageResolver resolverFor(FieldKind kind) {
    return usageResolvers.stream()
        .filter(r -> r.supports(kind))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No usage resolver registered for " + kind));
  }

  private static FieldDefinitionResponse toResponse(FieldDefinition f) {
    return new FieldDefinitionResponse(
        f.getId(),
        f.getKind().name(),
        f.getProjectId(),
        f.getCode(),
        f.getLabel(),
        f.getColor(),
        f.getSortOrder(),
        f.getIsActive(),
        f.getIsDefault(),
        f.getIsDone(),
        f.getIsCancelled(),
        f.getIsActiveWork(),
        f.getIsDefect(),
        f.getCreatedAt(),
        f.getUpdatedAt());
  }

  private FieldDefinition require(Integer id, Integer projectId) {
    return (projectId != null
            ? repository.findByIdAndProjectId(id, projectId)
            : repository.findByIdAndProjectIdIsNull(id))
        .orElseThrow(() -> new FieldDefinitionNotFoundException(id));
  }

  /** Every active row for one project (or, when {@code projectId} is null, every global row). */
  @Transactional(readOnly = true)
  public List<FieldDefinitionResponse> list(Integer projectId, FieldKind kind) {
    if (kind != null) {
      validateScope(projectId, kind);
    }

    List<FieldDefinition> rows =
        kind != null
            ? (projectId != null
                ? repository.findByProjectIdAndKindAndIsActiveTrueOrderBySortOrder(
                    projectId, kind)
                : repository.findByProjectIdIsNullAndKindAndIsActiveTrueOrderBySortOrder(kind))
            : (projectId != null
                ? repository.findByProjectIdAndIsActiveTrueOrderByKindAscSortOrderAsc(projectId)
                : repository.findByProjectIdIsNullAndIsActiveTrueOrderByKindAscSortOrderAsc());

    return rows.stream().map(FieldDefinitionService::toResponse).toList();
  }

  /**
   * Rejects a caller who reaches this through the wrong controller - a project-scoped kind
   * ({@code ISSUE_STATUS}, ...) attempted through {@link GlobalFieldDefinitionController}, or a
   * global kind ({@code PROJECT_STATUS}, {@code TEAM_FIELD}) attempted through {@link
   * FieldDefinitionController}. Nothing downstream of this checks {@code kind.isGlobal()} against
   * {@code projectId} again, so every write path that takes a caller-supplied kind calls this
   * first.
   */
  private void validateScope(Integer projectId, FieldKind kind) {
    if (kind.isGlobal() && projectId != null) {
      throw new FieldDefinitionRuleViolationException(
          FieldDefErrorCode.FIELD_KIND_NOT_PROJECT_SCOPED);
    }
    if (!kind.isGlobal() && projectId == null) {
      throw new FieldDefinitionRuleViolationException(FieldDefErrorCode.FIELD_KIND_NOT_GLOBAL);
    }
  }

  @Transactional
  public FieldDefinitionResponse create(Integer projectId, FieldDefinitionCreateRequest request) {
    validateScope(projectId, request.kind());

    FieldDefinition field = new FieldDefinition();
    field.setKind(request.kind());
    field.setProjectId(projectId);
    field.setCode(request.code());
    field.setLabel(request.label());
    field.setColor(request.color());
    field.setSortOrder(nextSortOrder(projectId, request.kind()));
    field.setIsDefault(request.isDefault());
    field.setIsDone(request.isDone());
    field.setIsCancelled(request.isCancelled());
    field.setIsActiveWork(request.isActiveWork());
    field.setIsDefect(request.isDefect());

    if (request.isDefault()) {
      clearExistingDefault(projectId, request.kind());
    }

    FieldDefinition saved;
    try {
      saved = repository.save(field);
    } catch (DataIntegrityViolationException e) {
      throw new FieldCodeAlreadyExistsException(request.kind(), request.code());
    }

    eventPublisher.publishEvent(new FieldDefinitionsChangedEvent(projectId));

    return toResponse(saved);
  }

  @Transactional
  public FieldDefinitionResponse update(
      Integer projectId, Integer id, FieldDefinitionUpdateRequest request) {
    FieldDefinition field = require(id, projectId);

    boolean losingDefault = field.getIsDefault() && !request.isDefault();
    boolean losingDone = field.getIsDone() && !request.isDone();

    if (losingDefault) {
      requireAnotherDefaultExists(projectId, field.getKind());
    }
    if (losingDone && field.getKind() == FieldKind.ISSUE_STATUS) {
      requireAnotherDoneExists(projectId, field.getKind());
    }
    if (request.isDefault() && !field.getIsDefault()) {
      clearExistingDefault(projectId, field.getKind());
    }

    field.setLabel(request.label());
    field.setColor(request.color());
    field.setIsDefault(request.isDefault());
    field.setIsDone(request.isDone());
    field.setIsCancelled(request.isCancelled());
    field.setIsActiveWork(request.isActiveWork());
    field.setIsDefect(request.isDefect());

    FieldDefinition saved = repository.save(field);

    eventPublisher.publishEvent(new FieldDefinitionsChangedEvent(projectId));

    return toResponse(saved);
  }

  /** How many live rows (issues, sprints, ...) still carry this code - see {@link #delete}. */
  @Transactional(readOnly = true)
  public long usageCount(Integer projectId, Integer id) {
    FieldDefinition field = require(id, projectId);
    return resolverFor(field.getKind()).countUsages(field.getKind(), projectId, field.getCode());
  }

  /** Delete with no reassignment - fails with {@link FieldDefErrorCode#FIELD_IN_USE} if any row
   * still carries this code. */
  @Transactional
  public void delete(Integer projectId, Integer id) {
    delete(projectId, id, null);
  }

  /**
   * Soft delete - see {@code FieldDefinition}. Blocked when it would leave an invariant broken, or
   * when live rows still carry this code and the caller has not named {@code reassignTo} - an
   * active field definition of the same kind/scope, other than the one being deleted - to move
   * them onto first. This is what stops the "issues vanish from the board" failure mode: a status
   * with issues on it can no longer be retired out from under them silently.
   */
  @Transactional
  public void delete(Integer projectId, Integer id, String reassignTo) {
    FieldDefinition field = require(id, projectId);

    if (field.getIsDefault()) {
      requireAnotherDefaultExists(projectId, field.getKind());
    }
    if (field.getIsDone() && field.getKind() == FieldKind.ISSUE_STATUS) {
      requireAnotherDoneExists(projectId, field.getKind());
    }

    FieldCodeUsageResolver resolver = resolverFor(field.getKind());
    long usages = resolver.countUsages(field.getKind(), projectId, field.getCode());
    if (usages > 0) {
      if (reassignTo == null) {
        throw new FieldDefinitionRuleViolationException(FieldDefErrorCode.FIELD_IN_USE);
      }
      if (reassignTo.equals(field.getCode())) {
        throw new FieldDefinitionRuleViolationException(FieldDefErrorCode.REASSIGN_TARGET_INVALID);
      }
      FieldDefinition target =
          (projectId != null
                  ? repository.findByProjectIdAndKindAndCodeAndIsActiveTrue(
                      projectId, field.getKind(), reassignTo)
                  : repository.findByProjectIdIsNullAndKindAndCodeAndIsActiveTrue(
                      field.getKind(), reassignTo))
              .orElseThrow(
                  () ->
                      new FieldDefinitionRuleViolationException(
                          FieldDefErrorCode.REASSIGN_TARGET_INVALID));
      resolver.reassign(field.getKind(), projectId, field.getCode(), target.getCode());
    }

    field.setIsActive(false);
    repository.save(field);

    eventPublisher.publishEvent(new FieldDefinitionsChangedEvent(projectId));
  }

  @Transactional
  public List<FieldDefinitionResponse> reorder(Integer projectId, ReorderRequest request) {
    validateScope(projectId, request.kind());

    List<FieldDefinition> current =
        projectId != null
            ? repository.findByProjectIdAndKindAndIsActiveTrueOrderBySortOrder(
                projectId, request.kind())
            : repository.findByProjectIdIsNullAndKindAndIsActiveTrueOrderBySortOrder(
                request.kind());

    Set<Integer> currentIds = current.stream().map(FieldDefinition::getId).collect(Collectors.toSet());
    Set<Integer> requestedIds = Set.copyOf(request.orderedIds());
    if (!currentIds.equals(requestedIds)) {
      throw new FieldDefinitionRuleViolationException(FieldDefErrorCode.REORDER_SET_MISMATCH);
    }

    for (FieldDefinition field : current) {
      field.setSortOrder(request.orderedIds().indexOf(field.getId()));
    }
    List<FieldDefinition> saved = repository.saveAll(current);

    eventPublisher.publishEvent(new FieldDefinitionsChangedEvent(projectId));

    return saved.stream()
        .sorted(Comparator.comparing(FieldDefinition::getSortOrder))
        .map(FieldDefinitionService::toResponse)
        .toList();
  }

  private int nextSortOrder(Integer projectId, FieldKind kind) {
    List<FieldDefinition> existing =
        projectId != null
            ? repository.findByProjectIdAndKindAndIsActiveTrueOrderBySortOrder(projectId, kind)
            : repository.findByProjectIdIsNullAndKindAndIsActiveTrueOrderBySortOrder(kind);
    return existing.stream().mapToInt(FieldDefinition::getSortOrder).max().orElse(-1) + 1;
  }

  private void clearExistingDefault(Integer projectId, FieldKind kind) {
    (projectId != null
            ? repository.findByProjectIdAndKindAndIsDefaultTrueAndIsActiveTrue(projectId, kind)
            : repository.findByProjectIdIsNullAndKindAndIsDefaultTrueAndIsActiveTrue(kind))
        .ifPresent(
            existing -> {
              existing.setIsDefault(false);
              repository.save(existing);
            });
  }

  private void requireAnotherDefaultExists(Integer projectId, FieldKind kind) {
    long count =
        projectId != null
            ? repository.countByProjectIdAndKindAndIsDefaultTrueAndIsActiveTrue(projectId, kind)
            : repository.countByProjectIdIsNullAndKindAndIsDefaultTrueAndIsActiveTrue(kind);
    // count includes the row being changed, which still carries isDefault=true at this point
    if (count <= 1) {
      throw new FieldDefinitionRuleViolationException(FieldDefErrorCode.DEFAULT_FIELD_REQUIRED);
    }
  }

  private void requireAnotherDoneExists(Integer projectId, FieldKind kind) {
    long count = repository.countByProjectIdAndKindAndIsDoneTrueAndIsActiveTrue(projectId, kind);
    if (count <= 1) {
      throw new FieldDefinitionRuleViolationException(FieldDefErrorCode.LAST_DONE_FIELD_REQUIRED);
    }
  }
}
