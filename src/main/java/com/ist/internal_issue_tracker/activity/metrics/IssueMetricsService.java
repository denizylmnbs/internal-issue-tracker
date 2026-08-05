package com.ist.internal_issue_tracker.activity.metrics;

import com.ist.internal_issue_tracker.activity.exception.ActivityErrorCode;
import com.ist.internal_issue_tracker.activity.metrics.dto.BurndownResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.CumulativeFlowResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.DefectRatioResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.DurationStatsResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.FlowEfficiencyResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.MetricWindow;
import com.ist.internal_issue_tracker.activity.metrics.dto.NetFlowResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.ReopenRateResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.ThroughputBreakdownResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.ThroughputResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.TimeInStatusResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.VelocityResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.WipResponse;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import com.ist.internal_issue_tracker.shared.port.SprintSummary;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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
  private final SprintLookup sprintLookup;

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

  @Cacheable(cacheNames = "metrics-cycleTime")
  public DurationStatsResponse cycleTime(
      Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    return toResponse(
        window, issueMetricsRepository.cycleTime(projectId, window.from(), window.to()));
  }

  @Cacheable(cacheNames = "metrics-leadTime")
  public DurationStatsResponse leadTime(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    return toResponse(
        window, issueMetricsRepository.leadTime(projectId, window.from(), window.to()));
  }

  @Cacheable(cacheNames = "metrics-throughput")
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

  @Cacheable(cacheNames = "metrics-timeInStatus")
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

  @Cacheable(cacheNames = "metrics-flowEfficiency")
  public FlowEfficiencyResponse flowEfficiency(
      Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    FlowEfficiencyStats stats =
        issueMetricsRepository.flowEfficiency(projectId, window.from(), window.to());

    return new FlowEfficiencyResponse(
        window, stats.getFlowEfficiency(), stats.getActiveSeconds(), stats.getTotalSeconds());
  }

  @Cacheable(cacheNames = "metrics-reopenRate")
  public ReopenRateResponse reopenRate(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    ReopenStats stats = issueMetricsRepository.reopenStats(projectId, window.from(), window.to());

    return new ReopenRateResponse(
        window, stats.getDoneIssueCount(), stats.getReopenedIssueCount(), stats.getReopenRate());
  }

  /**
   * How many issues the aging list may name. Long enough to cover a stalled board, short enough that
   * the answer stays a list to act on rather than a second copy of the backlog.
   */
  private static final int AGING_LIMIT = 20;

  /**
   * Work in progress as it stands, plus the oldest of it by name.
   *
   * <p>Takes {@code asOf} instead of a window - see {@code WipStatusCount}. Two queries rather than
   * one because the two halves group differently and neither is a projection of the other.
   */
  @Cacheable(cacheNames = "metrics-wip")
  public WipResponse wip(Integer projectId, OffsetDateTime asOf) {
    if (!projectLookup.existsActiveProject(projectId)) {
      throw new AppException(ActivityErrorCode.PROJECT_NOT_FOUND);
    }

    OffsetDateTime resolvedAsOf = asOf != null ? asOf : OffsetDateTime.now();

    List<WipResponse.StatusEntry> byStatus =
        issueMetricsRepository.wipByStatus(projectId, resolvedAsOf).stream()
            .map(
                row ->
                    new WipResponse.StatusEntry(
                        row.getStatus(),
                        row.getIssueCount(),
                        row.getStoryPoints(),
                        row.getOldestAgeSeconds(),
                        row.getP50AgeSeconds()))
            .toList();

    List<WipResponse.AgingEntry> oldest =
        issueMetricsRepository.agingWip(projectId, resolvedAsOf, AGING_LIMIT).stream()
            .map(
                row ->
                    new WipResponse.AgingEntry(
                        row.getIssueId(),
                        row.getStatus(),
                        row.getEnteredAt().atOffset(ZoneOffset.UTC),
                        row.getAgeSeconds(),
                        row.getStoryPoint(),
                        row.getIssueType(),
                        row.getPriority()))
            .toList();

    return new WipResponse(resolvedAsOf, byStatus, oldest);
  }

  @Cacheable(cacheNames = "metrics-netFlow")
  public NetFlowResponse netFlow(
      Integer projectId, MetricsBucket bucket, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    List<NetFlowResponse.Point> points =
        issueMetricsRepository.netFlow(projectId, bucket.unit(), window.from(), window.to()).stream()
            .map(
                row ->
                    new NetFlowResponse.Point(
                        row.getBucketStart().atOffset(ZoneOffset.UTC),
                        row.getCreatedCount(),
                        row.getCompletedCount(),
                        row.getNetCount(),
                        row.getCumulativeNetCount()))
            .toList();

    return new NetFlowResponse(window, bucket, points);
  }

  @Cacheable(cacheNames = "metrics-throughputBreakdown")
  public ThroughputBreakdownResponse throughputBreakdown(
      Integer projectId,
      MetricsDimension dimension,
      MetricsBucket bucket,
      OffsetDateTime from,
      OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    List<ThroughputBreakdownResponse.Point> points =
        issueMetricsRepository
            .throughputBreakdown(
                projectId, dimension.name(), bucket.unit(), window.from(), window.to())
            .stream()
            .map(
                row ->
                    new ThroughputBreakdownResponse.Point(
                        row.getBucketStart().atOffset(ZoneOffset.UTC),
                        row.getDimensionValue(),
                        row.getCompletedCount(),
                        row.getCompletedPoints()))
            .toList();

    return new ThroughputBreakdownResponse(window, bucket, dimension, points);
  }

  @Cacheable(cacheNames = "metrics-defectRatio")
  public DefectRatioResponse defectRatio(
      Integer projectId, MetricsBucket bucket, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    List<DefectRatioResponse.Point> points =
        issueMetricsRepository
            .defectStats(projectId, bucket.unit(), window.from(), window.to())
            .stream()
            .map(
                row ->
                    new DefectRatioResponse.Point(
                        row.getBucketStart().atOffset(ZoneOffset.UTC),
                        row.getCreatedCount(),
                        row.getCreatedBugCount(),
                        row.getCreatedBugShare(),
                        row.getCompletedCount(),
                        row.getCompletedBugCount(),
                        row.getCompletedStoryPoints(),
                        row.getDefectsPerCompletedIssue(),
                        row.getDefectsPerCompletedPoint()))
            .toList();

    return new DefectRatioResponse(window, bucket, points);
  }

  @Cacheable(cacheNames = "metrics-bugMttr")
  public DurationStatsResponse bugMttr(
      Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    return toResponse(window, issueMetricsRepository.bugMttr(projectId, window.from(), window.to()));
  }

  /**
   * Delivered points from the activity log, joined to what each sprint committed to.
   *
   * <p>The sprint list drives the result, not the log: a sprint with nothing finished in it still
   * appears, reading zero, because a sprint that delivered nothing is the most important row on the
   * chart and letting it fall out would hide it. The consequence is the other way round too - work
   * whose {@code sprint_id} names a soft-deleted sprint is dropped, since there is nothing left to
   * label the row with.
   */
  @Cacheable(cacheNames = "metrics-velocity")
  public VelocityResponse velocity(Integer projectId) {
    if (!projectLookup.existsActiveProject(projectId)) {
      throw new AppException(ActivityErrorCode.PROJECT_NOT_FOUND);
    }

    Map<Integer, SprintVelocity> delivered =
        issueMetricsRepository.velocity(projectId).stream()
            .collect(Collectors.toMap(SprintVelocity::getSprintId, row -> row));

    List<VelocityResponse.Sprint> sprints =
        sprintLookup.findSprintSummaries(projectId).stream()
            .map(
                summary -> {
                  SprintVelocity row = delivered.get(summary.id());
                  long completedPoints = row != null ? row.getCompletedPoints() : 0L;
                  long completedIssues = row != null ? row.getCompletedIssueCount() : 0L;

                  return new VelocityResponse.Sprint(
                      summary.id(),
                      summary.name(),
                      summary.status(),
                      summary.startDate(),
                      summary.endDate(),
                      summary.committedPoints(),
                      completedPoints,
                      completedIssues,
                      sayDoRatio(summary.committedPoints(), completedPoints));
                })
            .toList();

    return new VelocityResponse(sprints);
  }

  /**
   * Null when there was no commitment to measure against, and null when the commitment was zero -
   * dividing by it would be undefined, and reporting a sprint that committed to nothing as a
   * spectacular over-delivery is worse than saying nothing.
   */
  private static Double sayDoRatio(Integer committedPoints, long completedPoints) {
    if (committedPoints == null || committedPoints == 0) {
      return null;
    }

    return (double) completedPoints / committedPoints;
  }

  /**
   * One sprint's burndown. The window comes from the sprint rather than from the caller - a burndown
   * over an arbitrary range is not a burndown - and stops at today so a running sprint does not
   * trail a flat line into the future.
   *
   * <p>A sprint with no end date runs to today, which is the same thing the client would draw anyway.
   */
  @Cacheable(cacheNames = "metrics-burndown")
  public BurndownResponse burndown(Integer projectId, Integer sprintId) {
    if (!projectLookup.existsActiveProject(projectId)) {
      throw new AppException(ActivityErrorCode.PROJECT_NOT_FOUND);
    }

    SprintSummary sprint =
        sprintLookup.findSprintSummaries(projectId).stream()
            .filter(candidate -> candidate.id().equals(sprintId))
            .findFirst()
            .orElseThrow(() -> new AppException(ActivityErrorCode.SPRINT_NOT_FOUND));

    OffsetDateTime from = sprint.startDate().atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime today = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime end =
        sprint.endDate() != null
            ? sprint.endDate().atStartOfDay().atOffset(ZoneOffset.UTC)
            : today;
    OffsetDateTime to = end.isBefore(today) ? end : today;

    List<BurndownResponse.Point> points =
        to.isBefore(from)
            ? List.of()
            : issueMetricsRepository.burndown(projectId, sprintId, from, to).stream()
                .map(
                    row ->
                        new BurndownResponse.Point(
                            row.getBucketStart().atOffset(ZoneOffset.UTC),
                            row.getRemainingPoints(),
                            row.getRemainingIssueCount(),
                            row.getCompletedPoints(),
                            row.getScopePoints()))
                .toList();

    return new BurndownResponse(
        sprint.id(),
        sprint.name(),
        sprint.status(),
        sprint.startDate(),
        sprint.endDate(),
        sprint.committedPoints(),
        points);
  }

  @Cacheable(cacheNames = "metrics-cumulativeFlow")
  public CumulativeFlowResponse cumulativeFlow(
      Integer projectId, OffsetDateTime from, OffsetDateTime to) {
    MetricWindow window = window(projectId, from, to);

    List<CumulativeFlowResponse.Point> points =
        issueMetricsRepository.cumulativeFlow(projectId, window.from(), window.to()).stream()
            .map(
                row ->
                    new CumulativeFlowResponse.Point(
                        row.getBucketStart().atOffset(ZoneOffset.UTC),
                        row.getStatus(),
                        row.getIssueCount()))
            .toList();

    return new CumulativeFlowResponse(window, points);
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
