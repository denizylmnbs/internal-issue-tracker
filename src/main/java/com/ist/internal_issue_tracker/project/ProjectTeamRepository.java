package com.ist.internal_issue_tracker.project;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectTeamRepository extends JpaRepository<ProjectTeam, Integer> {

  /**
   * The teams on a project. The {@code teams} join matches {@link #countActiveTeams}: soft-deleting
   * a team leaves its {@code project_teams} row active, so without it a deleted team stayed on the
   * list while the count beside it had already dropped to zero.
   */
  @Query(
      value =
          """
          SELECT pt.* FROM project_teams pt
            JOIN teams t ON t.id = pt.team_id AND t.is_active
           WHERE pt.project_id = :projectId AND pt.is_active
          """,
      countQuery =
          """
          SELECT count(*) FROM project_teams pt
            JOIN teams t ON t.id = pt.team_id AND t.is_active
           WHERE pt.project_id = :projectId AND pt.is_active
          """,
      nativeQuery = true)
  Page<ProjectTeam> findActiveTeamsOfProject(
      @Param("projectId") Integer projectId, Pageable pageable);

  /** Backed by {@code unique_active_project_team} - see {@code ProjectMemberRepository}. */
  Optional<ProjectTeam> findByProjectIdAndTeamIdAndIsActiveTrue(Integer projectId, Integer teamId);

  /**
   * The pair's latest assignment row, live or not, so putting a team back on a project revives its
   * old row instead of stacking another one behind it - see {@code ProjectMemberRepository}.
   */
  Optional<ProjectTeam> findFirstByProjectIdAndTeamIdOrderByIdDesc(
      Integer projectId, Integer teamId);

  /**
   * Native SQL only because a soft-deleted team must not be counted, and {@code teams} belongs to
   * another module - see the note on {@code ProjectMemberRepository}.
   */
  @Query(
      value =
          """
          SELECT count(*)
            FROM project_teams pt
            JOIN teams t ON t.id = pt.team_id AND t.is_active
           WHERE pt.project_id = :projectId AND pt.is_active
          """,
      nativeQuery = true)
  long countActiveTeams(@Param("projectId") Integer projectId);
}
