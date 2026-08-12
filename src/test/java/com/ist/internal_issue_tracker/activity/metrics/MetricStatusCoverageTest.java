package com.ist.internal_issue_tracker.activity.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.ist.internal_issue_tracker.issue.IssueStatus;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Guards the one coupling in this codebase that the compiler and {@code ModularityTests} both miss.
 *
 * <p>The metric queries match on status names as string literals, because statuses reach {@code
 * activity} as text in {@code issue_activities.new_value} and this module may not depend on {@code
 * issue} to learn them. {@link MetricStatus} writes those names down; nothing enforces that it
 * still agrees with {@code IssueStatus}.
 *
 * <p>Adding a status to {@code IssueStatus} without adding it here would not fail to compile and
 * would not fail any other test. It would simply fall outside every set in {@code MetricStatus},
 * and flow efficiency would carry on returning a plausible number that had stopped meaning what it
 * claims. This test is the only thing standing between that change and a silently wrong metric.
 *
 * <p>It can import both enums only because it is a test - {@code ModularityTests} inspects main
 * sources, which is exactly the boundary that forced the duplication.
 */
class MetricStatusCoverageTest {

  @Test
  void metricStatus_namesEveryIssueStatus() {
    Set<String> issueStatuses =
        Arrays.stream(IssueStatus.values()).map(Enum::name).collect(Collectors.toSet());
    Set<String> metricStatuses =
        Arrays.stream(MetricStatus.values()).map(Enum::name).collect(Collectors.toSet());

    assertThat(metricStatuses)
        .as(
            "MetricStatus must mirror IssueStatus - the metric SQL matches these names as literals, "
                + "so a status missing here is a status silently excluded from every metric")
        .isEqualTo(issueStatuses);
  }

  /**
   * The active set is what flow efficiency divides by, so a status landing in neither camp would be
   * counted as waiting without anyone deciding that it should be.
   */
  @Test
  void everyStatus_isEitherActiveOrDeliberatelyWaiting() {
    Set<MetricStatus> waiting =
        Set.of(
            MetricStatus.BACKLOG,
            MetricStatus.TODO,
            MetricStatus.ON_HOLD,
            MetricStatus.DONE,
            MetricStatus.CANCELLED);

    for (MetricStatus status : MetricStatus.values()) {
      assertThat(MetricStatus.ACTIVE.contains(status) || waiting.contains(status))
          .as("%s is in neither the active nor the waiting set - decide which it is", status)
          .isTrue();
    }
  }

  @Test
  void completed_excludesCancelled() {
    assertThat(MetricStatus.COMPLETED).containsExactly(MetricStatus.DONE);
  }
}
