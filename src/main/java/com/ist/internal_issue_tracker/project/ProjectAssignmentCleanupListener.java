package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.shared.event.ProjectDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.event.TeamDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.event.UserDeactivatedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps {@code project_users} and {@code project_teams} honest when a user, a team, or the project
 * itself is deleted.
 *
 * <p>This is what the membership reads in this module now rest on: an active assignment row means a
 * live user on a live project, because anything else has already been retired here. See {@code
 * TeamMembershipCleanupListener} for why the delivery is synchronous rather than asynchronous - a
 * {@code @link} would not resolve, the class being package-private in another module.
 *
 * <p>A deactivated user is not taken off the teams they were on - that is {@code team}'s row to
 * clear, and it does. The team route into a project reads {@code team_users}, so clearing it there
 * removes them from here too.
 */
@Component
@RequiredArgsConstructor
class ProjectAssignmentCleanupListener {

  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectTeamRepository projectTeamRepository;

  @EventListener
  void onUserDeactivated(UserDeactivatedEvent event) {
    projectMemberRepository.deactivateAllByUserId(event.userId(), OffsetDateTime.now());
  }

  @EventListener
  void onTeamDeactivated(TeamDeactivatedEvent event) {
    projectTeamRepository.deactivateAllByTeamId(event.teamId(), OffsetDateTime.now());
  }

  @EventListener
  void onProjectDeactivated(ProjectDeactivatedEvent event) {
    OffsetDateTime deactivatedAt = OffsetDateTime.now();

    projectMemberRepository.deactivateAllByProjectId(event.projectId(), deactivatedAt);
    projectTeamRepository.deactivateAllByProjectId(event.projectId(), deactivatedAt);
  }
}
