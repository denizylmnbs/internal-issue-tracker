package com.ist.internal_issue_tracker.sprint;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every lookup here is derived rather than native: a sprint's world is one table, and the project it
 * hangs off is validated through {@code ProjectLookup} before any of these run. Nothing crosses a
 * module boundary, so none of the reasons that pushed the membership repositories into raw SQL apply
 * - which also means sorting keeps working on the list endpoint.
 */
public interface SprintRepository extends JpaRepository<Sprint, Integer> {

  /**
   * Both keys, always. Looking a sprint up by id alone would let the leader of project A pass their
   * own project in the path and a sprint of project B alongside it: the authorization rule only ever
   * sees the project, so the mismatch has to be caught here or not at all.
   */
  Optional<Sprint> findByIdAndProjectIdAndDeletedAtIsNull(Integer id, Integer projectId);

  /**
   * Scoped to the project and blind to deleted rows, matching
   * {@code unique_active_sprint_name_per_project}: two projects may each have a "Sprint 1", and
   * deleting one hands its name back.
   */
  boolean existsByProjectIdAndNameAndDeletedAtIsNull(Integer projectId, String name);

  /** The same check for an update, excusing the row being updated. */
  boolean existsByProjectIdAndNameAndDeletedAtIsNullAndIdNot(
      Integer projectId, String name, Integer id);

  /**
   * Pre-check for {@code one_active_sprint_per_project}. That index is <em>not</em> partial on
   * {@code deleted_at} - it reserves the slot for any row reading {@code IN_PROGRESS}, deleted or
   * not. This method deliberately ignores deleted rows anyway, because it answers the question the
   * API cares about; keeping the two in agreement is {@code SprintService#deleteSprint}'s job, which
   * stops a running sprint before dropping it so no deleted row is ever left holding the slot.
   */
  boolean existsByProjectIdAndStatusAndDeletedAtIsNull(Integer projectId, SprintStatus status);

  /**
   * One project's sprints, deleted ones excluded unconditionally - see
   * {@code TeamRepository#findAllByFilters} for why that is not a caller-supplied filter, and for
   * why {@code :name} needs the {@code CAST} that {@code :status} does not.
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
      @Param("status") SprintStatus status,
      Pageable pageable);
}
