package com.ist.internal_issue_tracker.sprint;

import static org.assertj.core.api.Assertions.assertThat;

import com.ist.internal_issue_tracker.shared.event.SprintField;
import com.ist.internal_issue_tracker.shared.event.SprintFieldChange;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Mostly about the date pair, which is the one thing this detector does that the issue one does
 * not.
 */
class SprintChangeDetectorTest {

  private final SprintChangeDetector detector = new SprintChangeDetector();

  private static Sprint sprint() {
    Sprint sprint = new Sprint();
    sprint.setId(1);
    sprint.setProjectId(1);
    sprint.setName("Sprint 1");
    sprint.setDescription("First");
    sprint.setStartDate(LocalDate.of(2026, 1, 1));
    sprint.setEndDate(LocalDate.of(2026, 1, 15));
    sprint.setStatus("TODO");
    return sprint;
  }

  @Test
  void diff_isEmpty_whenNothingMoved() {
    Sprint sprint = sprint();

    assertThat(detector.diff(SprintSnapshot.of(sprint), sprint)).isEmpty();
  }

  @Test
  void diff_reportsStatus_withBothEnumNames() {
    Sprint sprint = sprint();
    SprintSnapshot before = SprintSnapshot.of(sprint);

    sprint.setStatus("IN_PROGRESS");

    assertThat(detector.diff(before, sprint))
        .containsExactly(new SprintFieldChange(SprintField.STATUS, "TODO", "IN_PROGRESS"));
  }

  /**
   * Moving either date is one DATES change carrying both, because the schema gives them one action.
   */
  @Test
  void diff_reportsDatesOnce_whenOnlyTheEndDateMoved() {
    Sprint sprint = sprint();
    SprintSnapshot before = SprintSnapshot.of(sprint);

    sprint.setEndDate(LocalDate.of(2026, 1, 22));

    assertThat(detector.diff(before, sprint))
        .containsExactly(
            new SprintFieldChange(
                SprintField.DATES, "2026-01-01..2026-01-15", "2026-01-01..2026-01-22"));
  }

  /** A sprint with no agreed end renders as an open range rather than as the string "null". */
  @Test
  void diff_rendersOpenRange_whenTheEndDateIsCleared() {
    Sprint sprint = sprint();
    SprintSnapshot before = SprintSnapshot.of(sprint);

    sprint.setEndDate(null);

    assertThat(detector.diff(before, sprint))
        .containsExactly(
            new SprintFieldChange(SprintField.DATES, "2026-01-01..2026-01-15", "2026-01-01.."));
  }

  @Test
  void diff_reportsDetailsWithoutValues_whenOnlyTheDescriptionMoved() {
    Sprint sprint = sprint();
    SprintSnapshot before = SprintSnapshot.of(sprint);

    sprint.setDescription("Rewritten");

    assertThat(detector.diff(before, sprint))
        .containsExactly(new SprintFieldChange(SprintField.DETAILS, null, null));
  }

  @Test
  void diff_reportsEveryFieldMovedByOneCall() {
    Sprint sprint = sprint();
    SprintSnapshot before = SprintSnapshot.of(sprint);

    sprint.setName("Sprint 1 - extended");
    sprint.setEndDate(LocalDate.of(2026, 1, 29));
    sprint.setStatus("COMPLETED");

    assertThat(detector.diff(before, sprint))
        .containsExactly(
            new SprintFieldChange(SprintField.DETAILS, "Sprint 1", "Sprint 1 - extended"),
            new SprintFieldChange(
                SprintField.DATES, "2026-01-01..2026-01-15", "2026-01-01..2026-01-29"),
            new SprintFieldChange(SprintField.STATUS, "TODO", "COMPLETED"));
  }
}
