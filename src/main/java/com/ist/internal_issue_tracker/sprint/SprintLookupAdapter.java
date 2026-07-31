package com.ist.internal_issue_tracker.sprint;

import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintLookupAdapter implements SprintLookup {
  private final SprintRepository sprintRepository;

  /** Leans on the same two-key lookup the service uses, so both answer "live sprint" alike. */
  @Override
  public boolean existsLiveSprintInProject(Integer projectId, Integer sprintId) {
    return projectId != null
        && sprintId != null
        && sprintRepository.findByIdAndProjectIdAndDeletedAtIsNull(sprintId, projectId).isPresent();
  }
}
