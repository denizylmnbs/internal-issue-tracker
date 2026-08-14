package com.ist.internal_issue_tracker.sprint;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every lookup here is derived rather than native: a sprint's world is one table, and the project
 * it hangs off is validated through {@code ProjectLookup} before any of these run. Nothing crosses
 * a module boundary, so none of the reasons that pushed the membership repositories into raw SQL
 * apply - which also means sorting keeps working on the list endpoint.
 */
interface SprintRepository extends JpaRepository<Sprint, Integer> {

  /**
   * Both keys, always. Looking a sprint up by id alone would let the leader of project A pass their
   * own project in the path and a sprint of project B alongside it: the authorization rule only
   * ever sees the project, so the mismatch has to be caught here or not at all.
   */
  Optional<Sprint> findByIdAndProjectIdAndDeletedAtIsNull(Integer id, Integer projectId);

  /**
   * One project's live sprints in the order they ran, for {@code SprintLookupAdapter} to hand
   * across as summaries. Unpaged on purpose - a velocity chart plots all of them - and the {@code
   * id} tie-break keeps two sprints starting the same day in a stable order rather than whichever
   * the planner happened to return.
   */
  List<Sprint> findAllByProjectIdAndDeletedAtIsNullOrderByStartDateAscIdAsc(Integer projectId);

  /**
   * Scoped to the project and blind to deleted rows, matching {@code
   * unique_active_sprint_name_per_project}: two projects may each have a "Sprint 1", and deleting
   * one hands its name back.
   */
  boolean existsByProjectIdAndNameAndDeletedAtIsNull(Integer projectId, String name);

  /** The same check for an update, excusing the row being updated. */
  boolean existsByProjectIdAndNameAndDeletedAtIsNullAndIdNot(
      Integer projectId, String name, Integer id);

  /**
   * Pre-check for {@code one_active_sprint_per_project}, whose {@code WHERE} clause this mirrors
   * term for term: that index covers {@code (project_id)} where {@code is_running} is true
   * <em>and</em> {@code deleted_at} is null. Dropping the {@code DeletedAtIsNull} here would make
   * this method stricter than the constraint it stands in for, refusing a sprint the database would
   * have accepted.
   */
  boolean existsByProjectIdAndIsRunningTrueAndDeletedAtIsNull(Integer projectId);

  /**
   * One project's sprints, deleted ones excluded unconditionally - see {@code
   * TeamRepository#findAllByFilters} for why that is not a caller-supplied filter, and for why
   * {@code :name} needs the {@code CAST} that {@code :status} does not.
   */
  @Query(
      """
      SELECT s FROM Sprint s
      WHERE s.projectId = :projectId
      AND s.deletedAt IS NULL
      AND (CAST(:name AS String) IS NULL
             OR lower(s.name) LIKE lower(concat('%', CAST(:name AS String), '%')))
      AND (:status IS NULL OR s.status = :status)
      """)
  Page<Sprint> findAllByFilters(
      @Param("projectId") Integer projectId,
      @Param("name") String name,
      @Param("status") String status,
      Pageable pageable);

  /** Usage count/reassignment for {@code fielddef}'s delete guard - see {@code
   * SprintFieldCodeUsageAdapter}. */
  long countByProjectIdAndStatusAndDeletedAtIsNull(Integer projectId, String status);

  List<Sprint> findByProjectIdAndStatusAndDeletedAtIsNull(Integer projectId, String status);
}
