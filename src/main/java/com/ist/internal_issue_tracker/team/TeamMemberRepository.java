package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.team.dto.UserTeamMembershipResponse;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {

  Page<TeamMember> findAllByTeamIdAndIsActiveTrue(Integer teamId, Pageable pageable);

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
          where tm.userId = :userId and tm.isActive = true
          """,
      countQuery =
          """
          select count(tm)
          from TeamMember tm
          join Team t on t.id = tm.teamId
          where tm.userId = :userId and tm.isActive = true
          """)
  Page<UserTeamMembershipResponse> findActiveMembershipsWithTeamByUserId(
      @Param("userId") Integer userId, Pageable pageable);
}
