package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.team.dto.UserTeamMembershipResponse;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {

  /**
   * Every live membership in the system. Native SQL because an active membership row is not enough
   * on its own - the team and the user behind it have to still exist. Soft-deleting a team does not
   * touch its {@code team_users} rows, so without the {@code teams} join a deleted team's roster
   * would go on being listed here after it had vanished from every other view.
   */
  @Query(
      value =
          """
          SELECT tu.* FROM team_users tu
            JOIN teams t ON t.id = tu.team_id AND t.is_active
            JOIN users u ON u.id = tu.user_id AND u.is_active
           WHERE tu.is_active
          """,
      countQuery =
          """
          SELECT count(*) FROM team_users tu
            JOIN teams t ON t.id = tu.team_id AND t.is_active
            JOIN users u ON u.id = tu.user_id AND u.is_active
           WHERE tu.is_active
          """,
      nativeQuery = true)
  Page<TeamMember> findAllActiveMemberships(Pageable pageable);

  /**
   * One team's roster. The {@code users} join is what keeps a deactivated user out of it - the
   * membership row stays active when a user is soft-deleted, and the counts elsewhere already
   * exclude them, so listing them here made the same person present and absent at once.
   */
  @Query(
      value =
          """
          SELECT tu.* FROM team_users tu
            JOIN users u ON u.id = tu.user_id AND u.is_active
           WHERE tu.team_id = :teamId AND tu.is_active
          """,
      countQuery =
          """
          SELECT count(*) FROM team_users tu
            JOIN users u ON u.id = tu.user_id AND u.is_active
           WHERE tu.team_id = :teamId AND tu.is_active
          """,
      nativeQuery = true)
  Page<TeamMember> findActiveMembersOfTeam(@Param("teamId") Integer teamId, Pageable pageable);

  /**
   * At most one row can match: {@code unique_active_team_membership} is a partial unique index over
   * {@code (team_id, user_id) where is_active}, so the pair identifies the live membership while
   * leaving any number of soft-deleted ones behind it.
   */
  Optional<TeamMember> findByTeamIdAndUserIdAndIsActiveTrue(Integer teamId, Integer userId);

  /**
   * The user's own memberships, joined to the owning team so a caller can render team names without
   * a lookup per row. {@code TeamMember} holds a plain {@code teamId} rather than a {@code @ManyToOne
   * Team} (see {@link Team}), so the join is expressed explicitly; both entities live in this module,
   * so it crosses no boundary. The count query is spelled out because Spring Data cannot derive one
   * from a constructor expression.
   */
  @Query(
      value =
          """
          select new com.ist.internal_issue_tracker.team.dto.UserTeamMembershipResponse(
              tm.id, t.id, t.name, t.field, tm.createdAt)
          from TeamMember tm
          join Team t on t.id = tm.teamId
          where tm.userId = :userId and tm.isActive = true and t.isActive = true
          """,
      countQuery =
          """
          select count(tm)
          from TeamMember tm
          join Team t on t.id = tm.teamId
          where tm.userId = :userId and tm.isActive = true and t.isActive = true
          """)
  Page<UserTeamMembershipResponse> findActiveMembershipsWithTeamByUserId(
      @Param("userId") Integer userId, Pageable pageable);
}
