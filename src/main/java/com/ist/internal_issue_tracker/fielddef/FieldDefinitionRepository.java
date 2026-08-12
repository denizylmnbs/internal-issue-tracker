package com.ist.internal_issue_tracker.fielddef;

import com.ist.internal_issue_tracker.shared.port.FieldKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code projectId} is split into an explicit {@code IsNull} variant everywhere rather than passed
 * as a possibly-null parameter to a plain {@code ProjectId} finder: Spring Data JPA binds a
 * derived-query parameter with {@code =}, and {@code column = NULL} never matches - it would
 * silently return nothing for every global row instead of finding them.
 */
interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, Integer> {

  List<FieldDefinition> findByProjectIdAndKindAndIsActiveTrueOrderBySortOrder(
      Integer projectId, FieldKind kind);

  List<FieldDefinition> findByProjectIdIsNullAndKindAndIsActiveTrueOrderBySortOrder(
      FieldKind kind);

  List<FieldDefinition> findByProjectIdAndIsActiveTrueOrderByKindAscSortOrderAsc(
      Integer projectId);

  List<FieldDefinition> findByProjectIdIsNullAndIsActiveTrueOrderByKindAscSortOrderAsc();

  Optional<FieldDefinition> findByIdAndProjectId(Integer id, Integer projectId);

  Optional<FieldDefinition> findByIdAndProjectIdIsNull(Integer id);

  Optional<FieldDefinition> findByProjectIdAndKindAndCodeAndIsActiveTrue(
      Integer projectId, FieldKind kind, String code);

  Optional<FieldDefinition> findByProjectIdIsNullAndKindAndCodeAndIsActiveTrue(
      FieldKind kind, String code);

  Optional<FieldDefinition> findByProjectIdAndKindAndIsDefaultTrueAndIsActiveTrue(
      Integer projectId, FieldKind kind);

  Optional<FieldDefinition> findByProjectIdIsNullAndKindAndIsDefaultTrueAndIsActiveTrue(
      FieldKind kind);

  List<FieldDefinition> findByProjectIdAndKindAndIsDoneTrueAndIsActiveTrue(
      Integer projectId, FieldKind kind);

  List<FieldDefinition> findByProjectIdAndKindAndIsCancelledTrueAndIsActiveTrue(
      Integer projectId, FieldKind kind);

  List<FieldDefinition> findByProjectIdAndKindAndIsActiveWorkTrueAndIsActiveTrue(
      Integer projectId, FieldKind kind);

  List<FieldDefinition> findByProjectIdAndKindAndIsDefectTrueAndIsActiveTrue(
      Integer projectId, FieldKind kind);

  long countByProjectIdAndKindAndIsDoneTrueAndIsActiveTrue(Integer projectId, FieldKind kind);

  long countByProjectIdAndKindAndIsDefaultTrueAndIsActiveTrue(Integer projectId, FieldKind kind);

  long countByProjectIdIsNullAndKindAndIsDefaultTrueAndIsActiveTrue(FieldKind kind);
}
