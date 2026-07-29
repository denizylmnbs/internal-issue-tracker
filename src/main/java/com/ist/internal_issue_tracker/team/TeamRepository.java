package com.ist.internal_issue_tracker.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Integer> {

  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Integer id);

  boolean existsByIdAndLeaderId(Integer id, Integer leaderId);
  /**
   * The {@code CAST(:name AS String)} wrappers are load-bearing: a bare {@code :name IS NULL} gives
   * Hibernate no context to infer the parameter type from, so the driver sends it untyped and
   * PostgreSQL fails the statement with {@code function lower(bytea) does not exist}. {@code
   * :field} and {@code :leaderId} need no cast - comparing them against a typed column is enough
   * for both Hibernate and PostgreSQL to infer the type.
   */
  @Query(
      """
            SELECT t FROM Team t
            WHERE (CAST(:name AS String) IS NULL
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
