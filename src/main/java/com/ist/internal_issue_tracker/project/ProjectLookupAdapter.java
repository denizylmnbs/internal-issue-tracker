package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectLookupAdapter implements ProjectLookup {
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;

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

  @Override
  public boolean isParticipantOfProject(Integer projectId, Integer userId) {
    return projectId != null
        && userId != null
        && projectMemberRepository.existsActiveParticipant(projectId, userId);
  }
}
