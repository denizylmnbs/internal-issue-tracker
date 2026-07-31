package com.ist.internal_issue_tracker.epic;

import com.ist.internal_issue_tracker.shared.port.EpicLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EpicLookupAdapter implements EpicLookup {
  private final EpicRepository epicRepository;

  /** Leans on the same two-key lookup the service uses, so both answer "live epic" alike. */
  @Override
  public boolean existsLiveEpicInProject(Integer projectId, Integer epicId) {
    return projectId != null
        && epicId != null
        && epicRepository.findByIdAndProjectIdAndDeletedAtIsNull(epicId, projectId).isPresent();
  }
}
