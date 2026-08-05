package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectLookupAdapter implements ProjectLookup {
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectTeamRepository projectTeamRepository;
  private final TeamLookup teamLookup;

  @Cacheable(
      cacheNames = "project-leader",
      key = "#projectId + ':' + #userId",
      condition = "#projectId != null && #userId != null")
  @Override
  public boolean isLeaderOfProject(Integer projectId, Integer userId) {
    return userId != null
        && projectId != null
        && projectRepository.existsByIdAndLeaderId(projectId, userId);
  }

  @Override
  public boolean existsActiveProject(Integer projectId) {
    return projectId != null && projectRepository.existsByIdAndIsActiveTrue(projectId);
  }

  /**
   * The two routes onto a project, checked in the order that answers soonest. A direct assignment
   * settles it in one query; only when there is none does the team route cost the trip to {@code
   * team} for the user's team ids and a second query here.
   *
   * <p>No {@code isActive} check on the project itself, matching what the previous single-query
   * version answered: deleting a project retires its assignment rows, so a deleted one has nothing
   * left for either check to find.
   */
  @Cacheable(
      cacheNames = "project-participant",
      key = "#projectId + ':' + #userId",
      condition = "#projectId != null && #userId != null")
  @Override
  public boolean isParticipantOfProject(Integer projectId, Integer userId) {
    if (projectId == null || userId == null) {
      return false;
    }

    if (projectMemberRepository.existsByProjectIdAndUserIdAndIsActiveTrue(projectId, userId)) {
      return true;
    }

    Set<Integer> teamIds = teamLookup.activeTeamIdsOfUser(userId);

    // an empty IN list is not valid SQL, and a user on no team has no team route anyway
    return !teamIds.isEmpty()
        && projectTeamRepository.existsByProjectIdAndTeamIdInAndIsActiveTrue(projectId, teamIds);
  }
}
