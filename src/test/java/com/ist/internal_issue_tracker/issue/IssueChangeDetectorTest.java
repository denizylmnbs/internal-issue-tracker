package com.ist.internal_issue_tracker.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.ist.internal_issue_tracker.shared.event.IssueField;
import com.ist.internal_issue_tracker.shared.event.IssueFieldChange;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * No mocks and no context: the detector takes two values and returns a list, so these are two
 * values and a list. What is being pinned down is mostly the null handling - every field but the
 * name and the status is nullable, and "was empty, now set" has to come out as a change rather than
 * as nothing.
 */
class IssueChangeDetectorTest {

  private final IssueChangeDetector detector = new IssueChangeDetector();

  /** An issue with every audited field populated, so a test only has to state what it moves. */
  private static Issue issue() {
    Issue issue = new Issue();
    issue.setId(1);
    issue.setProjectId(1);
    issue.setName("Login fails");
    issue.setDescription("Reproduced on staging");
    issue.setType("BUG");
    issue.setStatus("TODO");
    issue.setPriority("HIGH");
    issue.setStoryPoint(3);
    issue.setSprintId(10);
    issue.setAssigneeUserId(7);
    issue.setAssigneeTeamId(4);
    return issue;
  }

  @Test
  void diff_isEmpty_whenNothingMoved() {
    Issue issue = issue();

    assertThat(detector.diff(IssueSnapshot.of(issue), issue)).isEmpty();
  }

  /**
   * The case the whole no-op guard exists for: an update that restates every field with the value
   * it already had must not reach the activity log.
   */
  @Test
  void diff_isEmpty_whenEveryFieldIsRestatedWithItsOwnValue() {
    Issue before = issue();
    Issue after = issue();

    assertThat(detector.diff(IssueSnapshot.of(before), after)).isEmpty();
  }

  @Test
  void diff_reportsStatus_withBothEnumNames() {
    Issue issue = issue();
    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setStatus("IN_PROGRESS");

    assertThat(detector.diff(before, issue))
        .containsExactly(new IssueFieldChange(IssueField.STATUS, "TODO", "IN_PROGRESS"));
  }

  @Test
  void diff_reportsEveryFieldMovedByOneCall() {
    Issue issue = issue();
    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setName("Login broken");
    issue.setPriority("CRITICAL");
    issue.setStoryPoint(5);
    issue.setSprintId(11);

    assertThat(detector.diff(before, issue))
        .extracting(IssueFieldChange::field, IssueFieldChange::oldValue, IssueFieldChange::newValue)
        .containsExactly(
            tuple(IssueField.DETAILS, "Login fails", "Login broken"),
            tuple(IssueField.PRIORITY, "HIGH", "CRITICAL"),
            tuple(IssueField.STORY_POINT, "3", "5"),
            tuple(IssueField.SPRINT, "10", "11"));
  }

  /**
   * Name, description and type collapse into one action type. Only a name change has a rendering
   * that fits the value columns, so the other two are recorded as having happened without claiming
   * to say to what - see {@code IssueChangeDetector#addDetails}.
   */
  @Test
  void diff_reportsDetailsWithoutValues_whenOnlyTheDescriptionMoved() {
    Issue issue = issue();
    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setDescription("Also on production");

    assertThat(detector.diff(before, issue))
        .containsExactly(new IssueFieldChange(IssueField.DETAILS, null, null));
  }

  @Test
  void diff_reportsDetailsWithoutValues_whenOnlyTheTypeMoved() {
    Issue issue = issue();
    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setType("TASK");

    assertThat(detector.diff(before, issue))
        .containsExactly(new IssueFieldChange(IssueField.DETAILS, null, null));
  }

  /** One DETAILS row even though three fields moved, because the schema gives them one action. */
  @Test
  void diff_reportsDetailsOnce_whenNameDescriptionAndTypeAllMoved() {
    Issue issue = issue();
    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setName("Login broken");
    issue.setDescription("Also on production");
    issue.setType("TASK");

    assertThat(detector.diff(before, issue))
        .containsExactly(new IssueFieldChange(IssueField.DETAILS, "Login fails", "Login broken"));
  }

  @Test
  void diff_reportsAssignees_whenTheyAreCleared() {
    Issue issue = issue();
    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setAssigneeUserId(null);
    issue.setAssigneeTeamId(null);

    assertThat(detector.diff(before, issue))
        .containsExactly(
            new IssueFieldChange(IssueField.ASSIGNEE_USER, "7", null),
            new IssueFieldChange(IssueField.ASSIGNEE_TEAM, "4", null));
  }

  /** The other direction: an unassigned issue picking someone up is a change, not an absence. */
  @Test
  void diff_reportsAssignee_whenOneIsSetForTheFirstTime() {
    Issue issue = issue();
    issue.setAssigneeUserId(null);

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setAssigneeUserId(9);

    assertThat(detector.diff(before, issue))
        .containsExactly(new IssueFieldChange(IssueField.ASSIGNEE_USER, null, "9"));
  }

  @Test
  void diff_reportsStoryPoint_whenAnUnsizedIssueIsSized() {
    Issue issue = issue();
    issue.setStoryPoint(null);

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setStoryPoint(8);

    assertThat(detector.diff(before, issue))
        .containsExactly(new IssueFieldChange(IssueField.STORY_POINT, null, "8"));
  }

  @Test
  void diff_reportsSprint_whenAnIssueLeavesItsSprint() {
    Issue issue = issue();
    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setSprintId(null);

    assertThat(detector.diff(before, issue))
        .containsExactly(new IssueFieldChange(IssueField.SPRINT, "10", null));
  }

  /** The epic has no action type in {@code issue_activities}, so moving it records nothing. */
  @Test
  void diff_isEmpty_whenOnlyTheEpicMoved() {
    Issue issue = issue();
    issue.setEpicId(2);

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setEpicId(3);

    List<IssueFieldChange> changes = detector.diff(before, issue);

    assertThat(changes).isEmpty();
  }
}
