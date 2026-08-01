package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.shared.port.IssueLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueLookupAdapter implements IssueLookup {
  private final IssueRepository issueRepository;

  /** Leans on the same two-key lookup the service uses, so both answer "live issue" alike. */
  @Override
  public boolean existsLiveIssueInProject(Integer projectId, Integer issueId) {
    return projectId != null
        && issueId != null
        && issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId).isPresent();
  }

  @Override
  public int sumStoryPointsInSprint(Integer projectId, Integer sprintId) {
    if (projectId == null || sprintId == null) {
      return 0;
    }

    return issueRepository.sumStoryPointsInSprint(projectId, sprintId);
  }
}
