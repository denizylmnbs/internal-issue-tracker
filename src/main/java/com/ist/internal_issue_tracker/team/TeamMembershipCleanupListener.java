package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.event.TeamDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.event.UserDeactivatedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps {@code team_users} honest when the thing on either end of a membership goes away.
 *
 * <p>Every roster read in this module trusts {@code is_active} on the membership row alone. That
 * only holds if a deactivated user and a deleted team take their rows down with them, which is what
 * this does - once, at delete time, instead of a join on every read.
 *
 * <p>Plain {@code @EventListener}, and the events it consumes are not externalised: delivery over a
 * broker would leave a window in which a deleted team's roster is still being served. The listener
 * runs inline in the publisher's transaction, so the delete and the cleanup commit together or not
 * at all.
 */
@Component
@RequiredArgsConstructor
class TeamMembershipCleanupListener {

  private final TeamMemberRepository teamMemberRepository;

  @EventListener
  void onUserDeactivated(UserDeactivatedEvent event) {
    teamMemberRepository.deactivateAllByUserId(event.userId(), OffsetDateTime.now());
  }

  @EventListener
  void onTeamDeactivated(TeamDeactivatedEvent event) {
    teamMemberRepository.deactivateAllByTeamId(event.teamId(), OffsetDateTime.now());
  }
}
