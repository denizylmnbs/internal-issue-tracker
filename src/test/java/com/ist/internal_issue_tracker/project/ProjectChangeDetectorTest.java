package com.ist.internal_issue_tracker.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.ist.internal_issue_tracker.shared.event.ProjectField;
import com.ist.internal_issue_tracker.shared.event.ProjectFieldChange;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** The leader is the interesting part here: it is nullable on both sides and both nulls mean something. */
class ProjectChangeDetectorTest {

  private final ProjectChangeDetector detector = new ProjectChangeDetector();

  private static Project project() {
    Project project = new Project();
    project.setId(1);
    project.setName("Apollo");
    project.setDescription("Moon");
    project.setStartDate(LocalDate.of(2026, 1, 1));
    project.setEndDate(LocalDate.of(2026, 6, 1));
    project.setLeaderId(2);
    project.setStatus(ProjectStatus.PLANNING);
    return project;
  }

  @Test
  void diff_isEmpty_whenNothingMoved() {
    Project project = project();

    assertThat(detector.diff(ProjectSnapshot.of(project), project)).isEmpty();
  }

  /** A project may be handed back to nobody, and that has to record as a change rather than vanish. */
  @Test
  void diff_reportsLeader_whenItIsRemoved() {
    Project project = project();
    ProjectSnapshot before = ProjectSnapshot.of(project);

    project.setLeaderId(null);

    assertThat(detector.diff(before, project))
        .containsExactly(new ProjectFieldChange(ProjectField.LEADER, "2", null));
  }

  @Test
  void diff_reportsLeader_whenALeaderlessProjectGetsOne() {
    Project project = project();
    project.setLeaderId(null);

    ProjectSnapshot before = ProjectSnapshot.of(project);

    project.setLeaderId(5);

    assertThat(detector.diff(before, project))
        .containsExactly(new ProjectFieldChange(ProjectField.LEADER, null, "5"));
  }

  /**
   * The dates fold into DETAILS here, unlike on a sprint - {@code project_activities} has no dates
   * action of its own.
   */
  @Test
  void diff_reportsDetailsWithoutValues_whenOnlyTheEndDateMoved() {
    Project project = project();
    ProjectSnapshot before = ProjectSnapshot.of(project);

    project.setEndDate(LocalDate.of(2026, 9, 1));

    assertThat(detector.diff(before, project))
        .containsExactly(new ProjectFieldChange(ProjectField.DETAILS, null, null));
  }

  @Test
  void diff_reportsEveryFieldMovedByOneCall() {
    Project project = project();
    ProjectSnapshot before = ProjectSnapshot.of(project);

    project.setName("Apollo II");
    project.setLeaderId(9);
    project.setStatus(ProjectStatus.ACTIVE);

    assertThat(detector.diff(before, project))
        .containsExactly(
            new ProjectFieldChange(ProjectField.DETAILS, "Apollo", "Apollo II"),
            new ProjectFieldChange(ProjectField.LEADER, "2", "9"),
            new ProjectFieldChange(ProjectField.STATUS, "PLANNING", "ACTIVE"));
  }
}
