package com.ist.internal_issue_tracker.project;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Direct assignments, plus the two queries that answer "who actually works on this project" by
 * folding in the teams. Both of those are native SQL: {@code project_teams} is this module's, but
 * {@code team_users} belongs to {@code team} and mapping it here would drag team internals into
 * {@code project}. The trade was made deliberately - the cost is that these two queries know those
 * table names by hand and no test of the module structure can see it, so they are kept side by side
 * rather than scattered.
 */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {

  /**
   * One project's direct assignments. The {@code users} join keeps a deactivated user out, matching
   * what {@link #countActiveMembers} and {@link #findActiveParticipants} already do - without it the
   * same person was listed here and missing from the count beside it.
   */
  @Query(
      value =
          """
          SELECT pu.* FROM project_users pu
            JOIN users u ON u.id = pu.user_id AND u.is_active
           WHERE pu.project_id = :projectId AND pu.is_active
          """,
      countQuery =
          """
          SELECT count(*) FROM project_users pu
            JOIN users u ON u.id = pu.user_id AND u.is_active
           WHERE pu.project_id = :projectId AND pu.is_active
          """,
      nativeQuery = true)
  Page<ProjectMember> findActiveMembersOfProject(
      @Param("projectId") Integer projectId, Pageable pageable);

  /**
   * At most one row can match: {@code unique_active_project_user} is a partial unique index over
   * {@code (user_id, project_id) where is_active}, so the pair identifies the live assignment while
   * leaving any number of soft-deleted ones behind it.
   */
  Optional<ProjectMember> findByProjectIdAndUserIdAndIsActiveTrue(
      Integer projectId, Integer userId);

  /**
   * The pair's latest assignment row, live or not, so re-adding someone who was taken off the
   * project revives their old row instead of stacking another one behind it. The partial index only
   * keeps the active rows unique, so several soft-deleted ones may sit behind the newest.
   */
  Optional<ProjectMember> findFirstByProjectIdAndUserIdOrderByIdDesc(
      Integer projectId, Integer userId);

  /**
   * Everyone working on the project, counted once. {@code UNION} removes the overlap between the two
   * routes, which is the whole reason this is not two additions.
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

  /**
   * Whether one user works on the project, by either route. The same union as {@link
   * #countActiveMembers} narrowed to a single person, so the two can never disagree about who
   * counts.
   *
   * <p>{@code UNION ALL} rather than {@code UNION}: someone assigned both directly and through a
   * team would produce two rows, and deduplicating them costs work to reach an answer that is
   * {@code true} either way. No {@code Pageable}, so the sort hazard that comes with native paging
   * does not arise here.
   */
  @Query(
      value =
          """
          SELECT EXISTS (
              SELECT 1
                FROM project_users pu
                JOIN users u ON u.id = pu.user_id AND u.is_active
               WHERE pu.project_id = :projectId AND pu.user_id = :userId AND pu.is_active
              UNION ALL
              SELECT 1
                FROM project_teams pt
                JOIN teams t ON t.id = pt.team_id AND t.is_active
                JOIN team_users tu ON tu.team_id = pt.team_id AND tu.is_active
                JOIN users u ON u.id = tu.user_id AND u.is_active
               WHERE pt.project_id = :projectId AND tu.user_id = :userId AND pt.is_active
          )
          """,
      nativeQuery = true)
  boolean existsActiveParticipant(
      @Param("projectId") Integer projectId, @Param("userId") Integer userId);

  /**
   * The same population as {@link #countActiveMembers}, listed rather than counted, with a flag for
   * how each person got there. {@code UNION ALL} plus {@code bool_or} rather than {@code UNION}: a
   * user who is both directly assigned and on an assigned team must appear once, and the direct
   * route has to win the flag.
   */
  @Query(
      value =
          """
          SELECT s.user_id AS user_id, bool_or(s.direct) AS directly_assigned FROM (
              SELECT pu.user_id, true AS direct
                FROM project_users pu
                JOIN users u ON u.id = pu.user_id AND u.is_active
               WHERE pu.project_id = :projectId AND pu.is_active
              UNION ALL
              SELECT tu.user_id, false AS direct
                FROM project_teams pt
                JOIN teams t ON t.id = pt.team_id AND t.is_active
                JOIN team_users tu ON tu.team_id = pt.team_id AND tu.is_active
                JOIN users u ON u.id = tu.user_id AND u.is_active
               WHERE pt.project_id = :projectId AND pt.is_active
          ) s
          GROUP BY s.user_id
          """,
      countQuery =
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
  Page<ProjectParticipant> findActiveParticipants(
      @Param("projectId") Integer projectId, Pageable pageable);

  /**
   * The same union read from the other end: the projects one user works on, each carrying enough of
   * itself to render a list without a lookup per row. Soft-deleted projects drop out, so a user's
   * assignment to a deleted project is simply not reported.
   */
  @Query(
      value =
          """
          SELECT s.project_id AS project_id,
                 p.name AS project_name,
                 p.status AS project_status,
                 bool_or(s.direct) AS directly_assigned
            FROM (
              SELECT pu.project_id, true AS direct
                FROM project_users pu
               WHERE pu.user_id = :userId AND pu.is_active
              UNION ALL
              SELECT pt.project_id, false AS direct
                FROM project_teams pt
                JOIN teams t ON t.id = pt.team_id AND t.is_active
                JOIN team_users tu ON tu.team_id = pt.team_id AND tu.is_active
               WHERE tu.user_id = :userId AND pt.is_active
            ) s
            JOIN projects p ON p.id = s.project_id AND p.is_active
           GROUP BY s.project_id, p.name, p.status
          """,
      countQuery =
          """
          SELECT count(*) FROM (
              SELECT pu.project_id
                FROM project_users pu
                JOIN projects p ON p.id = pu.project_id AND p.is_active
               WHERE pu.user_id = :userId AND pu.is_active
              UNION
              SELECT pt.project_id
                FROM project_teams pt
                JOIN projects p ON p.id = pt.project_id AND p.is_active
                JOIN teams t ON t.id = pt.team_id AND t.is_active
                JOIN team_users tu ON tu.team_id = pt.team_id AND tu.is_active
               WHERE tu.user_id = :userId AND pt.is_active
          ) projects
          """,
      nativeQuery = true)
  Page<UserProject> findActiveProjectsByUserId(
      @Param("userId") Integer userId, Pageable pageable);
}
