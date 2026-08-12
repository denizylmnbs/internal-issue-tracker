package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.team.dto.UserTeamMembershipResponse;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * No native SQL. The roster queries used to join {@code teams} and {@code users} to keep rows of a
 * deleted team or a deactivated user off the list; those rows are now retired at the moment of the
 * delete - see {@link TeamMembershipCleanupListener} - so {@code is_active} on the membership row
 * is the whole truth and the reads are plain derived queries. Sorting works on the list endpoints
 * again as a result, which native paging had made impossible.
 */
interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {

  /** Every live membership in the system. */
  Page<TeamMember> findAllByIsActiveTrue(Pageable pageable);

  /** One team's roster. */
  Page<TeamMember> findAllByTeamIdAndIsActiveTrue(Integer teamId, Pageable pageable);

  /**
   * At most one row can match: {@code unique_active_team_membership} is a partial unique index over
   * {@code (team_id, user_id) where is_active}, so the pair identifies the live membership while
   * leaving any number of soft-deleted ones behind it.
   */
  Optional<TeamMember> findByTeamIdAndUserIdAndIsActiveTrue(Integer teamId, Integer userId);

  /**
   * The pair's latest membership row, live or not, so re-adding someone who was removed can revive
   * their old row instead of stacking another one behind it. Unlike the lookup above this one is
   * not covered by the partial index - nothing stops several soft-deleted rows for the same pair,
   * rows this method's own use is meant to stop accumulating - so it takes the newest and leaves
   * whatever history predates it alone.
   */
  Optional<TeamMember> findFirstByTeamIdAndUserIdOrderByIdDesc(Integer teamId, Integer userId);

  /**
   * The user's own memberships, joined to the owning team so a caller can render team names without
   * a lookup per row. {@code TeamMember} holds a plain {@code teamId} rather than a
   * {@code @ManyToOne Team} (see {@link Team}), so the join is expressed explicitly; both entities
   * live in this module, so it crosses no boundary. The count query is spelled out because Spring
   * Data cannot derive one from a constructor expression - and needs no join at all, since it
   * selects nothing from the team.
   *
   * <p>No {@code t.isActive} check: deleting a team retires its membership rows, so an active
   * membership already implies a live team. Repeating the condition would only invite the two to
   * disagree.
   *
   * <p>{@code joinedAt} comes from {@code updatedAt}, not {@code createdAt}: a membership that was
   * removed and granted again is the same row revived, so its creation date is when the person
   * first joined years ago rather than when they came back. Only active rows reach this query and
   * the only things that ever write to one are joining, leaving, and the cleanup below - which only
   * ever deactivates - so for every row returned here the last write was a join.
   */
  @Query(
      value =
          """
          select new com.ist.internal_issue_tracker.team.dto.UserTeamMembershipResponse(
              tm.id, t.id, t.name, t.field, tm.updatedAt)
          from TeamMember tm
          join Team t on t.id = tm.teamId
          where tm.userId = :userId and tm.isActive = true
          """,
      countQuery =
          """
          select count(tm)
          from TeamMember tm
          where tm.userId = :userId and tm.isActive = true
          """)
  Page<UserTeamMembershipResponse> findActiveMembershipsWithTeamByUserId(
      @Param("userId") Integer userId, Pageable pageable);

  /**
   * The ids of the teams a user is on, for {@code TeamLookupAdapter} to hand to {@code project}. A
   * projection rather than the rows themselves - the caller matches ids against its own assignments
   * and has no use for a {@code TeamMember}.
   */
  @Query("select tm.teamId from TeamMember tm where tm.userId = :userId and tm.isActive = true")
  Set<Integer> findActiveTeamIdsByUserId(@Param("userId") Integer userId);

  /** The mirror projection, for {@code TeamLookupAdapter#activeUserIdsOfTeam}. */
  @Query("select tm.userId from TeamMember tm where tm.teamId = :teamId and tm.isActive = true")
  Set<Integer> findActiveUserIdsByTeamId(@Param("teamId") Integer teamId);

  /**
   * Retires every live membership a user holds, in one statement. Driven by {@code
   * UserDeactivatedEvent}; the reads above depend on this having happened.
   *
   * <p>{@code updatedAt} is set by hand because a bulk update never goes through the entity, so
   * {@code @UpdateTimestamp} does not fire. It is passed in rather than taken from {@code
   * current_timestamp} so one deactivation stamps every row it touches with the same instant.
   *
   * <p>{@code flushAutomatically} pushes the caller's own pending change out first, and {@code
   * clearAutomatically} drops the now-stale entities the statement went around.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update TeamMember tm
      set tm.isActive = false, tm.updatedAt = :deactivatedAt
      where tm.userId = :userId and tm.isActive = true
      """)
  int deactivateAllByUserId(
      @Param("userId") Integer userId, @Param("deactivatedAt") OffsetDateTime deactivatedAt);

  /** The same, for every member of a team that has just been deleted. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update TeamMember tm
      set tm.isActive = false, tm.updatedAt = :deactivatedAt
      where tm.teamId = :teamId and tm.isActive = true
      """)
  int deactivateAllByTeamId(
      @Param("teamId") Integer teamId, @Param("deactivatedAt") OffsetDateTime deactivatedAt);
}
