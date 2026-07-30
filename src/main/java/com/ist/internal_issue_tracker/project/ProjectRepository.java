package com.ist.internal_issue_tracker.project;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

  /**
   * Deliberately blind to {@code isActive}: {@code projects.name} carries a plain UNIQUE constraint,
   * not a partial one, so a soft-deleted project still owns its name and the pre-check has to agree
   * with what the database will do.
   */
  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Integer id);

  /**
   * A soft-deleted project is treated as gone everywhere the API reads or writes one, so lookups go
   * through these rather than the inherited {@code findById}/{@code existsById}.
   */
  Optional<Project> findByIdAndIsActiveTrue(Integer id);

  boolean existsByIdAndIsActiveTrue(Integer id);

  /** Backs the "editor or the leader of this project" authorization rule. */
  boolean existsByIdAndLeaderId(Integer id, Integer leaderId);

  /**
   * Soft-deleted projects are excluded unconditionally - see {@code TeamRepository#findAllByFilters}
   * for the reasoning. Note that this is {@code isActive}, the delete flag, and has nothing to do
   * with {@code status}: a CANCELLED project is still listed, because it still exists.
   *
   * <p>{@code endDateBefore} drops open-ended projects, since a null {@code end_date} cannot be
   * shown to fall before anything.
   *
   * <p>The {@code CAST}s are load-bearing. A bare {@code :param IS NULL} gives PostgreSQL nothing to
   * resolve the placeholder's type from, and it rejects the statement with {@code could not
   * determine data type of parameter}. {@code :status} and {@code :leaderId} escape this because
   * Hibernate binds them with an explicit type; the dates and {@code :name} do not, so each needs
   * the cast to be spelled out. Verified by running the query, not by reading it.
   */
  @Query(
      """
            SELECT p FROM Project p
            WHERE p.isActive = true
            AND (CAST(:name AS String) IS NULL
                   OR lower(p.name) LIKE lower(concat('%', CAST(:name AS String), '%')))
            AND (:status IS NULL OR p.status = :status)
            AND (:leaderId IS NULL OR p.leaderId = :leaderId)
            AND (CAST(:startDateAfter AS LocalDate) IS NULL OR p.startDate >= :startDateAfter)
            AND (CAST(:endDateBefore AS LocalDate) IS NULL OR p.endDate <= :endDateBefore)
            """)
  Page<Project> findAllByFilters(
      @Param("name") String name,
      @Param("status") ProjectStatus status,
      @Param("leaderId") Integer leaderId,
      @Param("startDateAfter") LocalDate startDateAfter,
      @Param("endDateBefore") LocalDate endDateBefore,
      Pageable pageable);

  /**
   * Everyone working on the project, counted once: directly assigned users plus everyone reached
   * through an assigned team. {@code UNION} already removes the overlap between the two, which is
   * the whole reason this is not two additions.
   *
   * <p>Native SQL because {@code project_users}, {@code project_teams} and {@code team_users} have
   * no entities in this module - and mapping them here would put team internals inside {@code
   * project}. The cost is that this query knows those table names by hand; it has to be revisited
   * when those modules are built.
   */
  @Query(
      value =
          """
          SELECT count(*) FROM (
              SELECT pu.user_id
                FROM project_users pu
                JOIN users u ON u.id = pu.user_id AND u.is_active
               WHERE pu.project_id = :projectId AND pu.is_active
              UNION
              SELECT tu.user_id
                FROM project_teams pt
                JOIN teams t ON t.id = pt.team_id AND t.is_active
                JOIN team_users tu ON tu.team_id = pt.team_id AND tu.is_active
                JOIN users u ON u.id = tu.user_id AND u.is_active
               WHERE pt.project_id = :projectId AND pt.is_active
          ) members
          """,
      nativeQuery = true)
  long countActiveMembers(@Param("projectId") Integer projectId);

  @Query(
      value =
          """
          SELECT count(*)
            FROM project_teams pt
            JOIN teams t ON t.id = pt.team_id AND t.is_active
           WHERE pt.project_id = :projectId AND pt.is_active
          """,
      nativeQuery = true)
  long countActiveTeams(@Param("projectId") Integer projectId);
}
