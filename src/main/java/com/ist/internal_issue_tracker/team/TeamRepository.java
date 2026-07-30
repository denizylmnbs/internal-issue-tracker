package com.ist.internal_issue_tracker.team;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Integer> {

  /**
   * Deliberately blind to {@code isActive}: {@code teams.name} carries a plain UNIQUE constraint,
   * not a partial one, so a soft-deleted team still owns its name and the pre-check has to agree
   * with what the database will do.
   */
  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Integer id);

  /**
   * A soft-deleted team is treated as gone everywhere the API reads or writes one, so lookups go
   * through these rather than the inherited {@code findById}/{@code existsById}.
   */
  Optional<Team> findByIdAndIsActiveTrue(Integer id);

  boolean existsByIdAndIsActiveTrue(Integer id);

  boolean existsByIdAndLeaderId(Integer id, Integer leaderId);
  /**
   * Soft-deleted teams are excluded unconditionally rather than through a filter parameter: a
   * deleted team is not a team the API has anything to say about, and letting a caller opt back into
   * them would undo the delete for every reader.
   *
   * <p>The {@code CAST(:name AS String)} wrappers are load-bearing: a bare {@code :name IS NULL}
   * gives Hibernate no context to infer the parameter type from, so the driver sends it untyped and
   * PostgreSQL fails the statement with {@code function lower(bytea) does not exist}. {@code :field}
   * and {@code :leaderId} need no cast - comparing them against a typed column is enough for both
   * Hibernate and PostgreSQL to infer the type.
   */
  @Query(
      """
            SELECT t FROM Team t
            WHERE t.isActive = true
            AND (CAST(:name AS String) IS NULL
                   OR lower(t.name) LIKE lower(concat('%', CAST(:name AS String), '%')))
            AND (:field IS NULL OR t.field = :field)
            AND (:leaderId IS NULL OR t.leaderId = :leaderId)
            """)
  Page<Team> findAllByFilters(
      @Param("name") String name,
      @Param("field") TeamField field,
      @Param("leaderId") Integer leaderId,
      Pageable pageable);
}
