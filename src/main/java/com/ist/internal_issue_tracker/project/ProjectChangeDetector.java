package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.shared.event.ProjectField;
import com.ist.internal_issue_tracker.shared.event.ProjectFieldChange;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Works out what moved between a snapshot and the project it came from - the project counterpart of
 * {@code IssueChangeDetector}, and free of Spring, JPA and clock for the same reason.
 */
@Component
class ProjectChangeDetector {

  List<ProjectFieldChange> diff(ProjectSnapshot before, Project after) {
    List<ProjectFieldChange> changes = new ArrayList<>();

    addDetails(changes, before, after);

    if (!Objects.equals(before.leaderId(), after.getLeaderId())) {
      changes.add(
          new ProjectFieldChange(
              ProjectField.LEADER, number(before.leaderId()), number(after.getLeaderId())));
    }

    if (before.status() != after.getStatus()) {
      changes.add(
          new ProjectFieldChange(
              ProjectField.STATUS, name(before.status()), name(after.getStatus())));
    }

    return changes;
  }

  /**
   * Name, description and both dates share one action type, because {@code project_activities} gives
   * them one - it has no dates action of its own, unlike {@code sprint_activities}. Only a name
   * change is rendered into the value columns, for the reason given on {@code
   * IssueChangeDetector#addDetails}.
   */
  private static void addDetails(
      List<ProjectFieldChange> changes, ProjectSnapshot before, Project after) {
    boolean nameChanged = !Objects.equals(before.name(), after.getName());

    boolean detailsChanged =
        nameChanged
            || !Objects.equals(before.description(), after.getDescription())
            || !Objects.equals(before.startDate(), after.getStartDate())
            || !Objects.equals(before.endDate(), after.getEndDate());

    if (!detailsChanged) {
      return;
    }

    changes.add(
        new ProjectFieldChange(
            ProjectField.DETAILS,
            nameChanged ? before.name() : null,
            nameChanged ? after.getName() : null));
  }

  private static String name(Enum<?> value) {
    return value == null ? null : value.name();
  }

  private static String number(Integer value) {
    return value == null ? null : String.valueOf(value);
  }
}
