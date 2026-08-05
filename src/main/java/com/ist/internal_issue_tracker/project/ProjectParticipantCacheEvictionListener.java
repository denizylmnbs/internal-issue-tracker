package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.shared.event.ProjectMembershipEvent;
import com.ist.internal_issue_tracker.shared.event.TeamDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.event.TeamMembershipEvent;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Keeps {@code ProjectLookupAdapter#isParticipantOfProject}'s cache honest. That cache is keyed by
 * {@code projectId + ':' + userId}, so a change that names a single user on a single project evicts
 * one key - but a change that names a team fans out over everyone it currently reaches, since the
 * team route folds every member's participant status into that same boolean.
 *
 * <p>Plain {@code @EventListener}, not {@code @ApplicationModuleListener}: this runs inline in the
 * publisher's transaction, the same choice {@code ProjectAssignmentCleanupListener} makes and for the
 * same reason - an async gap here would let a removed member's cached {@code true} outlive the
 * removal by however long delivery takes, on top of the two-minute TTL that already bounds it.
 */
@Component
@RequiredArgsConstructor
class ProjectParticipantCacheEvictionListener {

  private final CacheManager cacheManager;
  private final ProjectTeamRepository projectTeamRepository;
  private final TeamLookup teamLookup;

  private Cache participantCache() {
    return cacheManager.getCache("project-participant");
  }

  private void evict(Integer projectId, Integer userId) {
    Cache cache = participantCache();
    if (cache != null) {
      cache.evict(projectId + ":" + userId);
    }
  }

  /**
   * Direct membership names one user; a team assignment reaches everyone currently on that team, so
   * the eviction is exactly the population the boolean's team route would have matched.
   */
  @EventListener
  void onProjectMembershipChanged(ProjectMembershipEvent event) {
    if (event.subject() == ProjectMembershipEvent.Subject.USER) {
      evict(event.projectId(), event.subjectId());
      return;
    }

    Set<Integer> memberIds = teamLookup.activeUserIdsOfTeam(event.subjectId());
    for (Integer userId : memberIds) {
      evict(event.projectId(), userId);
    }
  }

  /**
   * A user's own team roster changed, independent of any single project - so the fan-out runs the
   * other way, over every project the team is currently on.
   */
  @EventListener
  void onTeamMembershipChanged(TeamMembershipEvent event) {
    Set<Integer> projectIds = projectTeamRepository.findActiveProjectIdsByTeamId(event.teamId());
    for (Integer projectId : projectIds) {
      evict(projectId, event.userId());
    }
  }

  /**
   * The team's own roster and its project links, read while both still say {@code true}. {@code
   * TeamMembershipCleanupListener} and {@code ProjectAssignmentCleanupListener} answer the same
   * queries with nothing the moment they run - deleting a team is exactly what deactivates those
   * rows - so this must see them first. {@code @Order} makes that explicit rather than leaving it to
   * incidental bean-registration order, which {@code @EventListener} does not otherwise guarantee.
   */
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @EventListener
  void onTeamDeactivated(TeamDeactivatedEvent event) {
    Set<Integer> projectIds = projectTeamRepository.findActiveProjectIdsByTeamId(event.teamId());
    if (projectIds.isEmpty()) {
      return;
    }

    Set<Integer> memberIds = teamLookup.activeUserIdsOfTeam(event.teamId());
    for (Integer projectId : projectIds) {
      for (Integer userId : memberIds) {
        evict(projectId, userId);
      }
    }
  }
}
