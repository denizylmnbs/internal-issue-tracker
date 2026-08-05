package com.ist.internal_issue_tracker.project;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * No native SQL. Both reads used to join {@code teams} - another module's table, spelled out by hand
 * - only to drop assignments of a team that had been deleted. Those rows are now retired when the
 * team is, by {@link ProjectAssignmentCleanupListener}, so {@code is_active} on the assignment row
 * answers the question on its own.
 */
interface ProjectTeamRepository extends JpaRepository<ProjectTeam, Integer> {

  /** The teams on a project. */
  Page<ProjectTeam> findAllByProjectIdAndIsActiveTrue(Integer projectId, Pageable pageable);

  /** Its count, over exactly the same rows - the two can no longer drift apart. */
  long countByProjectIdAndIsActiveTrue(Integer projectId);

  /**
   * Whether any of the given teams is on the project - the team half of the participant question,
   * paired with {@code ProjectMemberRepository#existsByProjectIdAndUserIdAndIsActiveTrue}. The ids
   * come from {@code TeamLookup#activeTeamIdsOfUser}, so this module never reads {@code team_users}
   * to answer it. Must not be called with an empty collection; {@link ProjectLookupAdapter} checks.
   */
  boolean existsByProjectIdAndTeamIdInAndIsActiveTrue(
      Integer projectId, Collection<Integer> teamIds);

  /**
   * The projects a team is currently on, for {@code ProjectParticipantCacheEvictionListener} to fan
   * an eviction out over when a {@code TeamMembershipEvent} names that team - a user joining or
   * leaving it can change their participant status on every one of these at once.
   */
  @Query(
      "select pt.projectId from ProjectTeam pt where pt.teamId = :teamId and pt.isActive = true")
  Set<Integer> findActiveProjectIdsByTeamId(@Param("teamId") Integer teamId);

  /** Backed by {@code unique_active_project_team} - see {@code ProjectMemberRepository}. */
  Optional<ProjectTeam> findByProjectIdAndTeamIdAndIsActiveTrue(Integer projectId, Integer teamId);

  /**
   * The pair's latest assignment row, live or not, so putting a team back on a project revives its
   * old row instead of stacking another one behind it - see {@code ProjectMemberRepository}.
   */
  Optional<ProjectTeam> findFirstByProjectIdAndTeamIdOrderByIdDesc(
      Integer projectId, Integer teamId);

  /**
   * Takes a deleted team off every project it was on. Driven by {@code TeamDeactivatedEvent}; the
   * reads above depend on this having happened. See {@code
   * TeamMemberRepository#deactivateAllByUserId} for why {@code updatedAt} is passed in and why both
   * {@code @Modifying} flags are set.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update ProjectTeam pt
      set pt.isActive = false, pt.updatedAt = :deactivatedAt
      where pt.teamId = :teamId and pt.isActive = true
      """)
  int deactivateAllByTeamId(
      @Param("teamId") Integer teamId, @Param("deactivatedAt") OffsetDateTime deactivatedAt);

  /** The same, for every team on a project that has just been deleted. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update ProjectTeam pt
      set pt.isActive = false, pt.updatedAt = :deactivatedAt
      where pt.projectId = :projectId and pt.isActive = true
      """)
  int deactivateAllByProjectId(
      @Param("projectId") Integer projectId, @Param("deactivatedAt") OffsetDateTime deactivatedAt);
}
