package com.ist.internal_issue_tracker.issue;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Derived and JPQL throughout, no native SQL. An issue points at four other modules but only ever by
 * id - those references are validated through ports, never joined - so every query here stays inside
 * one table and sorting keeps working on the list endpoint.
 */
interface IssueRepository extends JpaRepository<Issue, Integer> {

  /**
   * Both keys, always. Looking an issue up by id alone would let the leader of project A pass their
   * own project in the path and an issue of project B alongside it: the authorization rule only ever
   * sees the project, so the mismatch has to be caught here or not at all.
   */
  Optional<Issue> findByIdAndProjectIdAndDeletedAtIsNull(Integer id, Integer projectId);

  /**
   * One project's issues, deleted ones excluded unconditionally. There is no name-uniqueness check
   * to pair this with - {@code issues} carries no unique index of any kind, so two issues on a
   * project may share a name and the filter below is a search rather than a lookup.
   *
   * <p>{@code :name} needs the {@code CAST} for the reason spelled out on
   * {@code TeamRepository#findAllByFilters}; the enums and ids are compared against typed columns
   * and need none.
   */
  @Query(
      """
      SELECT i FROM Issue i
      WHERE i.projectId = :projectId
      AND i.deletedAt IS NULL
      AND (CAST(:name AS String) IS NULL
             OR lower(i.name) LIKE lower(concat('%', CAST(:name AS String), '%')))
      AND (:type IS NULL OR i.type = :type)
      AND (:status IS NULL OR i.status = :status)
      AND (:priority IS NULL OR i.priority = :priority)
      AND (:sprintId IS NULL OR i.sprintId = :sprintId)
      AND (:epicId IS NULL OR i.epicId = :epicId)
      AND (:reporterId IS NULL OR i.reporterId = :reporterId)
      AND (:assigneeUserId IS NULL OR i.assigneeUserId = :assigneeUserId)
      AND (:assigneeTeamId IS NULL OR i.assigneeTeamId = :assigneeTeamId)
      """)
  Page<Issue> findAllByFilters(
      @Param("projectId") Integer projectId,
      @Param("name") String name,
      @Param("type") IssueType type,
      @Param("status") IssueStatus status,
      @Param("priority") IssuePriority priority,
      @Param("sprintId") Integer sprintId,
      @Param("epicId") Integer epicId,
      @Param("reporterId") Integer reporterId,
      @Param("assigneeUserId") Integer assigneeUserId,
      @Param("assigneeTeamId") Integer assigneeTeamId,
      Pageable pageable);
}
