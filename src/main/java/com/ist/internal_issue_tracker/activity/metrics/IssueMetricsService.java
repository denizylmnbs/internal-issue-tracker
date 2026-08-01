package com.ist.internal_issue_tracker.activity.metrics;

import com.ist.internal_issue_tracker.activity.exception.ActivityErrorCode;
import com.ist.internal_issue_tracker.activity.metrics.dto.DurationStatsResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.FlowEfficiencyResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.MetricWindow;
import com.ist.internal_issue_tracker.activity.metrics.dto.ReopenRateResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.ThroughputResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.TimeInStatusResponse;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The agile metrics, all of them derived from {@code issue_activities} and none of them stored.
 *
 * <p>Computing on read rather than maintaining a rollup is the right trade at this size: the queries
 * are indexed on {@code (project_id, action_type, created_at)} and a project's history is thousands
 * of rows, not millions. It also means a metric can be redefined by changing one query rather than
 * by rebuilding a table. When burndown and a cumulative flow diagram arrive they will not be able to
 * work this way - replaying the whole history on every draw is what a daily snapshot table exists to
 * avoid - but those are a different piece of work.
 */
@Service
@RequiredArgsConstructor
public class IssueMetricsService {

  /**
   * How far back an unbounded request looks. Long enough to hold several sprints, short enough that
   * a team's numbers reflect how it works now rather than how it worked last year.
   */
  private static final int DEFAULT_WINDOW_DAYS = 90;

  private final IssueMetricsRepository issueMetricsRepository;
  private final ProjectLookup projectLookup;

  /**
   * Resolves the window and checks the project in one place, because every metric needs both and
   * neither should be repeated six times.
   */
  private MetricWindow window(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    if (!projectLookup.existsActiveProject(projectId)) {
      throw new AppException(ActivityErrorCode.PROJECT_NOT_FOUND);
    }

    OffsetDateTime resolvedTo = to != null ? to : OffsetDateTime.now();
    OffsetDateTime resolvedFrom = from != null ? from : resolvedTo.minusDays(DEFAULT_WINDOW_DAYS);

    return new MetricWindow(resolvedFrom, resolvedTo);
  }

  public DurationStatsResponse cycleTime(
      Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    return toResponse(
        window, issueMetricsRepository.cycleTime(projectId, window.from(), window.to()));
  }

  public DurationStatsResponse leadTime(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    return toResponse(
        window, issueMetricsRepository.leadTime(projectId, window.from(), window.to()));
  }

  public ThroughputResponse throughput(
      Integer projectId, MetricsBucket bucket, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    List<ThroughputResponse.Point> points =
        issueMetricsRepository
            .throughput(projectId, bucket.unit(), window.from(), window.to())
            .stream()
            // the projection hands back an Instant - see ThroughputBucket
            .map(
                row ->
                    new ThroughputResponse.Point(
                        row.getBucketStart().atOffset(ZoneOffset.UTC), row.getCompletedCount()))
            .toList();

    return new ThroughputResponse(window, bucket, points);
  }

  public TimeInStatusResponse timeInStatus(
      Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    List<TimeInStatusResponse.Entry> entries =
        issueMetricsRepository.timeInStatus(projectId, window.from(), window.to()).stream()
            .map(
                row ->
                    new TimeInStatusResponse.Entry(
                        row.getStatus(),
                        row.getIssueCount(),
                        row.getTotalSeconds(),
                        row.getP50Seconds()))
            .toList();

    return new TimeInStatusResponse(window, entries);
  }

  public FlowEfficiencyResponse flowEfficiency(
      Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    FlowEfficiencyStats stats =
        issueMetricsRepository.flowEfficiency(projectId, window.from(), window.to());

    return new FlowEfficiencyResponse(
        window, stats.getFlowEfficiency(), stats.getActiveSeconds(), stats.getTotalSeconds());
  }

  public ReopenRateResponse reopenRate(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    ReopenStats stats = issueMetricsRepository.reopenStats(projectId, window.from(), window.to());

    return new ReopenRateResponse(
        window, stats.getDoneIssueCount(), stats.getReopenedIssueCount(), stats.getReopenRate());
  }

  private static DurationStatsResponse toResponse(MetricWindow window, DurationStats stats) {
    return new DurationStatsResponse(
        window,
        stats.getIssueCount(),
        stats.getAvgSeconds(),
        stats.getP50Seconds(),
        stats.getP85Seconds(),
        stats.getP95Seconds());
  }
}
