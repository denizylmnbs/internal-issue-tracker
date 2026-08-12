package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.shared.event.IssueField;
import com.ist.internal_issue_tracker.shared.event.IssueFieldChange;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Works out what actually changed between a snapshot and the issue it was taken from, so the
 * service can publish that rather than "someone called update".
 *
 * <p>The distinction earns its keep in two places. An update that restates every field with the
 * value it already had produces an empty list and therefore no event, keeping a log whose whole
 * value is that every row means something out of the business of recording non-events. And a single
 * call that moves several fields produces several changes, which is what lets one operation land as
 * the several action types the schema separates them into.
 *
 * <p>Deliberately free of Spring, JPA and clock: it takes two values and returns a list, so its
 * tests are two values and a list.
 */
@Component
class IssueChangeDetector {

  /**
   * Name, description and type share one action type, because {@code issue_activities} gives them
   * one. Only the name is rendered into the value columns: a description does not fit in 255
   * characters and truncating it would produce a record that looks like a value but is not one.
   *
   * <p>So a change to the description or the type alone is recorded with both values null - the row
   * says the details moved, without claiming to say to what. That is the null contract on {@code
   * IssueFieldChange}, and it is why the name is rendered only when the name is what changed.
   */
  private static void addDetails(
      List<IssueFieldChange> changes, IssueSnapshot before, Issue after) {
    boolean nameChanged = !Objects.equals(before.name(), after.getName());

    boolean detailsChanged =
        nameChanged
            || !Objects.equals(before.description(), after.getDescription())
            || before.type() != after.getType();

    if (!detailsChanged) {
      return;
    }

    changes.add(
        new IssueFieldChange(
            IssueField.DETAILS,
            nameChanged ? before.name() : null,
            nameChanged ? after.getName() : null));
  }

  private static String name(Enum<?> value) {
    return value == null ? null : value.name();
  }

  private static String number(Integer value) {
    return value == null ? null : String.valueOf(value);
  }

  /**
   * Compared in a fixed order so a multi-field change always reads the same way. Emptiness is the
   * meaningful case, not an edge one - see the class note.
   */
  List<IssueFieldChange> diff(IssueSnapshot before, Issue after) {
    List<IssueFieldChange> changes = new ArrayList<>();

    addDetails(changes, before, after);

    if (before.status() != after.getStatus()) {
      changes.add(
          new IssueFieldChange(IssueField.STATUS, name(before.status()), name(after.getStatus())));
    }

    if (before.priority() != after.getPriority()) {
      changes.add(
          new IssueFieldChange(
              IssueField.PRIORITY, name(before.priority()), name(after.getPriority())));
    }

    if (!Objects.equals(before.storyPoint(), after.getStoryPoint())) {
      changes.add(
          new IssueFieldChange(
              IssueField.STORY_POINT, number(before.storyPoint()), number(after.getStoryPoint())));
    }

    if (!Objects.equals(before.sprintId(), after.getSprintId())) {
      changes.add(
          new IssueFieldChange(
              IssueField.SPRINT, number(before.sprintId()), number(after.getSprintId())));
    }

    if (!Objects.equals(before.assigneeUserId(), after.getAssigneeUserId())) {
      changes.add(
          new IssueFieldChange(
              IssueField.ASSIGNEE_USER,
              number(before.assigneeUserId()),
              number(after.getAssigneeUserId())));
    }

    if (!Objects.equals(before.assigneeTeamId(), after.getAssigneeTeamId())) {
      changes.add(
          new IssueFieldChange(
              IssueField.ASSIGNEE_TEAM,
              number(before.assigneeTeamId()),
              number(after.getAssigneeTeamId())));
    }

    return changes;
  }
}
