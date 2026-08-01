package com.ist.internal_issue_tracker.epic;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Derived and JPQL throughout, no native SQL: an epic's world is one table, and the project it hangs
 * off is validated through {@code ProjectLookup} before any of these run. Nothing crosses a module
 * boundary, so sorting keeps working on the list endpoint - unlike the membership repositories.
 */
interface EpicRepository extends JpaRepository<Epic, Integer> {

  /**
   * Both keys, always. Looking an epic up by id alone would let the leader of project A pass their
   * own project in the path and an epic of project B alongside it: the authorization rule only ever
   * sees the project, so the mismatch has to be caught here or not at all.
   */
  Optional<Epic> findByIdAndProjectIdAndDeletedAtIsNull(Integer id, Integer projectId);

  /**
   * Scoped to the project and blind to deleted rows, matching
   * {@code unique_active_epic_name_per_project}: two projects may each have a "Checkout rewrite",
   * and deleting one hands its name back.
   */
  boolean existsByProjectIdAndNameAndDeletedAtIsNull(Integer projectId, String name);

  /** The same check for an update, excusing the row being updated. */
  boolean existsByProjectIdAndNameAndDeletedAtIsNullAndIdNot(
      Integer projectId, String name, Integer id);

  /**
   * One project's epics, deleted ones excluded unconditionally - see
   * {@code TeamRepository#findAllByFilters} for why that is not a caller-supplied filter, and for
   * why {@code :name} needs the {@code CAST} that {@code :status} and {@code :reporterId} do not.
   *
   * <p>{@code :reporterId} is what the schema's index on {@code epics(reporter_id)} is for; it
   * answers "the epics so-and-so opened" without a second endpoint existing to ask it.
   */
  @Query(
      """
      SELECT e FROM Epic e
      WHERE e.projectId = :projectId
      AND e.deletedAt IS NULL
      AND (CAST(:name AS String) IS NULL
             OR lower(e.name) LIKE lower(concat('%', CAST(:name AS String), '%')))
      AND (:status IS NULL OR e.status = :status)
      AND (:reporterId IS NULL OR e.reporterId = :reporterId)
      """)
  Page<Epic> findAllByFilters(
      @Param("projectId") Integer projectId,
      @Param("name") String name,
      @Param("status") EpicStatus status,
      @Param("reporterId") Integer reporterId,
      Pageable pageable);
}
