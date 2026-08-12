package com.ist.internal_issue_tracker.issue;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Derived and JPQL throughout, no native SQL. An issue points at four other modules but only ever
 * by id - those references are validated through ports, never joined - so every query here stays
 * inside one table and sorting keeps working on the list endpoint.
 */
interface IssueRepository extends JpaRepository<Issue, Integer> {

  /**
   * Both keys, always. Looking an issue up by id alone would let the leader of project A pass their
   * own project in the path and an issue of project B alongside it: the authorization rule only
   * ever sees the project, so the mismatch has to be caught here or not at all.
   */
  Optional<Issue> findByIdAndProjectIdAndDeletedAtIsNull(Integer id, Integer projectId);

  /**
   * What one sprint currently holds, in points. {@code coalesce} on both levels: the inner one
   * turns an unestimated issue into a zero rather than letting it drop out of the sum, and the
   * outer one turns a sprint with no issues at all into a zero rather than a null.
   *
   * <p>Read through {@code IssueLookup} by {@code sprint}, which is the only caller.
   */
  @Query(
      """
      SELECT coalesce(sum(coalesce(i.storyPoint, 0)), 0) FROM Issue i
      WHERE i.projectId = :projectId
      AND i.sprintId = :sprintId
      AND i.deletedAt IS NULL
      """)
  int sumStoryPointsInSprint(
      @Param("projectId") Integer projectId, @Param("sprintId") Integer sprintId);

  /**
   * One project's issues, deleted ones excluded unconditionally. There is no name-uniqueness check
   * to pair this with - {@code issues} carries no unique index of any kind, so two issues on a
   * project may share a name and the filter below is a search rather than a lookup.
   *
   * <p>{@code :name} needs the {@code CAST} for the reason spelled out on {@code
   * TeamRepository#findAllByFilters}; the enums and ids are compared against typed columns and need
   * none.
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
      AND (:resolvingUnit IS NULL OR i.resolvingUnit = :resolvingUnit)
      AND (:sprintId IS NULL OR i.sprintId = :sprintId)
      AND (:epicId IS NULL OR i.epicId = :epicId)
      AND (:reporterId IS NULL OR i.reporterId = :reporterId)
      AND (:assigneeUserId IS NULL OR i.assigneeUserId = :assigneeUserId)
      AND (:assigneeTeamId IS NULL OR i.assigneeTeamId = :assigneeTeamId)
      """)
  Page<Issue> findAllByFilters(
      @Param("projectId") Integer projectId,
      @Param("name") String name,
      @Param("type") String type,
      @Param("status") String status,
      @Param("priority") String priority,
      @Param("resolvingUnit") String resolvingUnit,
      @Param("sprintId") Integer sprintId,
      @Param("epicId") Integer epicId,
      @Param("reporterId") Integer reporterId,
      @Param("assigneeUserId") Integer assigneeUserId,
      @Param("assigneeTeamId") Integer assigneeTeamId,
      Pageable pageable);

  /**
   * Every live issue assigned to one person, across every project they hold any - unpaged and
   * un-projectId'd, because "My Work" is exactly the opposite of a project-scoped question.
   *
   * <p>Used to be a single {@code status IN (:statuses)} query against a fixed {@code IssueStatus}
   * set. Now that statuses are project-scoped data, "active" can mean a different set of codes on
   * every project this person touches, and this module cannot join {@code field_definitions} to
   * resolve that in SQL - {@code UserWorkService} does it in memory instead, against each issue's
   * own project. Read through {@code UserWorkService}, the only caller.
   */
  List<Issue> findByAssigneeUserIdAndDeletedAtIsNull(Integer assigneeUserId);

  /**
   * Every live, sprint-assigned issue for one person, across every project - the raw rows {@code
   * UserWorkService#getSprintProgress} groups and sums per project-and-sprint pair.
   *
   * <p>Used to be a {@code GROUP BY}/{@code CASE WHEN} aggregate query bound to a fixed {@code
   * DONE}/{@code CANCELLED} pair. That pair is project-scoped data now, for the same reason {@link
   * #findByAssigneeUserIdAndDeletedAtIsNull} moved out of SQL - see {@code UserWorkService}.
   */
  List<Issue> findByAssigneeUserIdAndDeletedAtIsNullAndSprintIdIsNotNull(Integer assigneeUserId);
}
