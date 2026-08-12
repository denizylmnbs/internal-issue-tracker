package com.ist.internal_issue_tracker.sprint;

import com.ist.internal_issue_tracker.shared.event.SprintField;
import com.ist.internal_issue_tracker.shared.event.SprintFieldChange;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Works out what moved between a snapshot and the sprint it came from - the sprint counterpart of
 * {@code IssueChangeDetector}, and free of Spring, JPA and clock for the same reason.
 */
@Component
class SprintChangeDetector {

  /**
   * Both dates in one value, because one action type covers the pair. An open end shows as bare.
   */
  private static String range(LocalDate start, LocalDate end) {
    return (start == null ? "" : start.toString()) + ".." + (end == null ? "" : end.toString());
  }

  List<SprintFieldChange> diff(SprintSnapshot before, Sprint after) {
    List<SprintFieldChange> changes = new ArrayList<>();

    boolean nameChanged = !Objects.equals(before.name(), after.getName());

    if (nameChanged || !Objects.equals(before.description(), after.getDescription())) {
      // only the name has a rendering worth storing - see IssueChangeDetector#addDetails
      changes.add(
          new SprintFieldChange(
              SprintField.DETAILS,
              nameChanged ? before.name() : null,
              nameChanged ? after.getName() : null));
    }

    if (!Objects.equals(before.startDate(), after.getStartDate())
        || !Objects.equals(before.endDate(), after.getEndDate())) {
      changes.add(
          new SprintFieldChange(
              SprintField.DATES,
              range(before.startDate(), before.endDate()),
              range(after.getStartDate(), after.getEndDate())));
    }

    if (!Objects.equals(before.status(), after.getStatus())) {
      changes.add(
          new SprintFieldChange(SprintField.STATUS, before.status(), after.getStatus()));
    }

    return changes;
  }
}
