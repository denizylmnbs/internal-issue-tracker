package com.ist.internal_issue_tracker.issue;

import java.util.Collection;
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
      @Param("type") IssueType type,
      @Param("status") IssueStatus status,
      @Param("priority") IssuePriority priority,
      @Param("resolvingUnit") IssueUnit resolvingUnit,
      @Param("sprintId") Integer sprintId,
      @Param("epicId") Integer epicId,
      @Param("reporterId") Integer reporterId,
      @Param("assigneeUserId") Integer assigneeUserId,
      @Param("assigneeTeamId") Integer assigneeTeamId,
      Pageable pageable);

  /**
   * One person's live issues across every project they hold any, filtered to the given statuses -
   * "My Work"'s active-issue list. Unlike every other query here there is no {@code projectId}: the
   * whole point is that a person's work is not confined to one.
   *
   * <p>Read through {@code UserWorkService}, the only caller.
   */
  @Query(
      """
      SELECT i FROM Issue i
      WHERE i.assigneeUserId = :assigneeUserId
      AND i.deletedAt IS NULL
      AND i.status IN :statuses
      """)
  Page<Issue> findByAssigneeAndStatuses(
      @Param("assigneeUserId") Integer assigneeUserId,
      @Param("statuses") Collection<IssueStatus> statuses,
      Pageable pageable);

  /**
   * One person's story points, summed per project-and-sprint pair, read from {@code issues}'
   * current state - see {@code UserSprintPoints} for why that is a deliberately different reading
   * from the project-wide velocity query. {@code CANCELLED} issues are excluded entirely, on both
   * sides of the sum: dropped work was never a commitment either finished or missed.
   *
   * <p>Unpaged and un-projectId'd like {@link #findByAssigneeAndStatuses}, for the same reason -
   * {@code UserWorkService} needs every sprint this person touched, anywhere, to build current,
   * previous and average progress.
   */
  @Query(
      """
      SELECT i.projectId AS projectId,
             i.sprintId AS sprintId,
             coalesce(sum(coalesce(i.storyPoint, 0)), 0) AS assignedPoints,
             coalesce(sum(CASE WHEN i.status = :doneStatus THEN coalesce(i.storyPoint, 0) ELSE 0 END), 0)
                 AS completedPoints,
             count(i) AS assignedIssueCount,
             sum(CASE WHEN i.status = :doneStatus THEN 1L ELSE 0L END) AS completedIssueCount
      FROM Issue i
      WHERE i.assigneeUserId = :assigneeUserId
      AND i.deletedAt IS NULL
      AND i.sprintId IS NOT NULL
      AND i.status <> :cancelledStatus
      GROUP BY i.projectId, i.sprintId
      """)
  List<UserSprintPoints> sprintPointsByAssignee(
      @Param("assigneeUserId") Integer assigneeUserId,
      @Param("doneStatus") IssueStatus doneStatus,
      @Param("cancelledStatus") IssueStatus cancelledStatus);
}
