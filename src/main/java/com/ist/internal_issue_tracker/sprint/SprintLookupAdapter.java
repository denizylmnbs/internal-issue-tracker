package com.ist.internal_issue_tracker.sprint;

import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import com.ist.internal_issue_tracker.shared.port.SprintSummary;
import java.util.List;
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

  /** Entities in, records out - {@code Sprint} never leaves this module. */
  @Override
  public List<SprintSummary> findSprintSummaries(Integer projectId) {
    if (projectId == null) {
      return List.of();
    }

    return sprintRepository
        .findAllByProjectIdAndDeletedAtIsNullOrderByStartDateAscIdAsc(projectId)
        .stream()
        .map(
            sprint ->
                new SprintSummary(
                    sprint.getId(),
                    sprint.getName(),
                    sprint.getStatus(),
                    sprint.getStartDate(),
                    sprint.getEndDate(),
                    sprint.getCommittedPoints(),
                    sprint.getCommittedAt()))
        .toList();
  }
}
