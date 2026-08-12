package com.ist.internal_issue_tracker.project;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Direct assignments, plus the queries that answer "who actually works on this project" by folding
 * in the teams.
 *
 * <p>Those last three are the only native SQL left in the application, and for one reason: a
 * participant is the union of two disjoint sources, and JPQL has no {@code UNION}. Everything else
 * that once forced raw SQL here - dropping rows of a deactivated user, a deleted team, a deleted
 * project - is now handled when the delete happens, by {@link ProjectAssignmentCleanupListener}, so
 * the surviving queries read {@code is_active} on the rows themselves and nothing more.
 *
 * <p>Two of them still name {@code team_users} by hand, which belongs to {@code team}: {@link
 * #countActiveMembers} and {@link #findActiveParticipants}. Both need the members of the assigned
 * teams, and they page over the result, so the ids cannot simply be fetched through {@code
 * TeamLookup} and passed in the way {@link #findActiveProjectsByUserId} now does - that would mean
 * carrying every member of every assigned team as a query parameter. The boundary is knowingly
 * crossed in these two and nowhere else in this module.
 */
interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {

  /** One project's direct assignments. */
  Page<ProjectMember> findAllByProjectIdAndIsActiveTrue(Integer projectId, Pageable pageable);

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
   * Everyone working on the project, counted once. {@code UNION} removes the overlap between the
   * two routes, which is the whole reason this is not two additions - and the whole reason it is
   * native.
   */
  @Query(
      value =
          """
          SELECT count(*) FROM (
              SELECT pu.user_id
                FROM project_users pu
               WHERE pu.project_id = :projectId AND pu.is_active
              UNION
              SELECT tu.user_id
                FROM project_teams pt
                JOIN team_users tu ON tu.team_id = pt.team_id AND tu.is_active
               WHERE pt.project_id = :projectId AND pt.is_active
          ) members
          """,
      nativeQuery = true)
  long countActiveMembers(@Param("projectId") Integer projectId);

  /**
   * The direct half of the participant question, asked on every request that names a project. The
   * team half is {@code ProjectTeamRepository#existsByProjectIdAndTeamIdInAndIsActiveTrue}, and
   * {@link ProjectLookupAdapter} is where the two are put together.
   *
   * <p>This used to be one JPQL query joining {@code TeamMember} - {@code team}'s entity, named
   * from inside {@code project}. Splitting it costs a second round trip only for users who are not
   * directly assigned; the direct case still answers in one.
   */
  boolean existsByProjectIdAndUserIdAndIsActiveTrue(Integer projectId, Integer userId);

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
               WHERE pu.project_id = :projectId AND pu.is_active
              UNION ALL
              SELECT tu.user_id, false AS direct
                FROM project_teams pt
                JOIN team_users tu ON tu.team_id = pt.team_id AND tu.is_active
               WHERE pt.project_id = :projectId AND pt.is_active
          ) s
          GROUP BY s.user_id
          """,
      countQuery =
          """
          SELECT count(*) FROM (
              SELECT pu.user_id
                FROM project_users pu
               WHERE pu.project_id = :projectId AND pu.is_active
              UNION
              SELECT tu.user_id
                FROM project_teams pt
                JOIN team_users tu ON tu.team_id = pt.team_id AND tu.is_active
               WHERE pt.project_id = :projectId AND pt.is_active
          ) s
          """,
      nativeQuery = true)
  Page<ProjectParticipant> findActiveParticipants(
      @Param("projectId") Integer projectId, Pageable pageable);

  /**
   * The same union read from the other end: the projects one user works on, each carrying enough of
   * itself to render a list without a lookup per row. The {@code projects} join is here for the
   * name and status, not to filter - deleting a project retires its assignment rows, so none of
   * them reach this query in the first place, which is also why the count query needs no join at
   * all.
   *
   * <p>The team route used to be a join onto {@code team_users}. It is now a plain {@code IN} over
   * team ids the caller has already resolved through {@code TeamLookup}, which leaves this query
   * reading nothing but {@code project}'s own tables. {@code teamIds} must not be empty - see
   * {@code ProjectMemberService#getProjectsByUserId}.
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
               WHERE pt.team_id IN (:teamIds) AND pt.is_active
            ) s
            JOIN projects p ON p.id = s.project_id
           GROUP BY s.project_id, p.name, p.status
          """,
      countQuery =
          """
          SELECT count(*) FROM (
              SELECT pu.project_id
                FROM project_users pu
               WHERE pu.user_id = :userId AND pu.is_active
              UNION
              SELECT pt.project_id
                FROM project_teams pt
               WHERE pt.team_id IN (:teamIds) AND pt.is_active
          ) s
          """,
      nativeQuery = true)
  Page<UserProject> findActiveProjectsByUserId(
      @Param("userId") Integer userId,
      @Param("teamIds") Collection<Integer> teamIds,
      Pageable pageable);

  /**
   * Takes a deactivated user off every project they were directly assigned to. Driven by {@code
   * UserDeactivatedEvent}; every read above depends on this having happened. See {@code
   * TeamMemberRepository#deactivateAllByUserId} for why {@code updatedAt} is passed in and why both
   * {@code @Modifying} flags are set.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update ProjectMember pu
      set pu.isActive = false, pu.updatedAt = :deactivatedAt
      where pu.userId = :userId and pu.isActive = true
      """)
  int deactivateAllByUserId(
      @Param("userId") Integer userId, @Param("deactivatedAt") OffsetDateTime deactivatedAt);

  /** The same, for every direct member of a project that has just been deleted. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update ProjectMember pu
      set pu.isActive = false, pu.updatedAt = :deactivatedAt
      where pu.projectId = :projectId and pu.isActive = true
      """)
  int deactivateAllByProjectId(
      @Param("projectId") Integer projectId, @Param("deactivatedAt") OffsetDateTime deactivatedAt);
}
