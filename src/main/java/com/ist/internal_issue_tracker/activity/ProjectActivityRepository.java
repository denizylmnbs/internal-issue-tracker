package com.ist.internal_issue_tracker.activity;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Append and read back - see {@link IssueActivityRepository}. */
interface ProjectActivityRepository extends JpaRepository<ProjectActivity, Integer> {

  /**
   * Guards the replayed event. Unlike the issue and sprint versions this also keys on the values:
   * two people can be added to a project in the same operation, producing rows that agree on
   * project, action and instant and differ only in who was added.
   */
  boolean existsByProjectIdAndActionTypeAndCreatedAtAndOldValueAndNewValue(
      Integer projectId,
      ProjectActionType actionType,
      OffsetDateTime createdAt,
      String oldValue,
      String newValue);

  Page<ProjectActivity> findAllByProjectId(Integer projectId, Pageable pageable);

  /**
   * Everything that happened on the project, from all three tables at once - not merely its own
   * history. {@code UNION ALL} rather than {@code UNION}: unlike {@code
   * ProjectMemberRepository#countActiveMembers}'s participant union, the three sources here are
   * disjoint by construction (a row belongs to exactly one table), so there is no overlap to remove
   * and a plain concatenation is cheaper.
   *
   * <p>Native for the same reason every other union query in this codebase is: JPQL has no {@code
   * UNION}. No {@code ORDER BY} in the query text itself - {@code pageable}'s sort is what supplies
   * it (see {@link ActivityService#feedNewestFirst}), the same way {@code
   * ProjectMemberRepository#findActiveParticipants} leaves sorting to the {@code Pageable} it is
   * handed rather than writing it into the SQL.
   */
  @Query(
      value =
          """
          SELECT id, user_id, action_type, old_value, new_value, created_at, scope, subject_id FROM (
              SELECT id, user_id, action_type, old_value, new_value, created_at,
                     'PROJECT' AS scope, project_id AS subject_id
                FROM project_activities
               WHERE project_id = :projectId
              UNION ALL
              SELECT id, user_id, action_type, old_value, new_value, created_at,
                     'ISSUE' AS scope, issue_id AS subject_id
                FROM issue_activities
               WHERE project_id = :projectId
              UNION ALL
              SELECT id, user_id, action_type, old_value, new_value, created_at,
                     'SPRINT' AS scope, sprint_id AS subject_id
                FROM sprint_activities
               WHERE project_id = :projectId
          ) feed
          """,
      countQuery =
          """
          SELECT count(*) FROM (
              SELECT id FROM project_activities WHERE project_id = :projectId
              UNION ALL
              SELECT id FROM issue_activities WHERE project_id = :projectId
              UNION ALL
              SELECT id FROM sprint_activities WHERE project_id = :projectId
          ) feed
          """,
      nativeQuery = true)
  Page<ActivityFeedRow> findFeedByProjectId(
      @Param("projectId") Integer projectId, Pageable pageable);
}
