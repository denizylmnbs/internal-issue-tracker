package com.ist.internal_issue_tracker.activity.metrics;

import com.ist.internal_issue_tracker.activity.metrics.dto.BurndownResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.CumulativeFlowResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.DefectRatioResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.DurationStatsResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.FlowEfficiencyResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.NetFlowResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.ReopenRateResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.ThroughputBreakdownResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.ThroughputResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.TimeInStatusResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.VelocityResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.WipResponse;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The agile metrics for one project. The path variable is named {@code id} because {@code
 * SecurityConfig} reads it literally - see {@code SprintController}.
 *
 * <p>{@code from} and {@code to} are optional everywhere and default to the last ninety days; the
 * window that was actually used comes back on every response, so a caller never has to guess.
 *
 * <p>Nothing here is paged. Five of the six return a single row, and throughput returns one point
 * per bucket over a bounded window - a shape a client charts whole rather than scrolls.
 *
 * <p>Open to project participants rather than to leads alone, which is a decision rather than an
 * oversight: a team that cannot see its own flow cannot improve it. The same reasoning is why these
 * are project-level aggregates and there is no per-person breakdown - a metric that becomes an
 * individual performance measure stops describing the work and starts describing how people respond
 * to being measured, which would corrupt the log everything here is computed from.
 */
@RestController
@RequestMapping("/api/projects/{id}/metrics")
@RequiredArgsConstructor
public class IssueMetricsController {

  private final IssueMetricsService issueMetricsService;

  /** How long work takes once it is started - first {@code IN_PROGRESS} to first {@code DONE}. */
  @GetMapping("/cycle-time")
  public ResponseEntity<ApiResponse<DurationStatsResponse>> getCycleTime(
      @PathVariable Integer id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.cycleTime(id, from, to)));
  }

  /** How long work takes from being asked for - {@code CREATED} to first {@code DONE}. */
  @GetMapping("/lead-time")
  public ResponseEntity<ApiResponse<DurationStatsResponse>> getLeadTime(
      @PathVariable Integer id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.leadTime(id, from, to)));
  }

  @GetMapping("/throughput")
  public ResponseEntity<ApiResponse<ThroughputResponse>> getThroughput(
      @PathVariable Integer id,
      @RequestParam(defaultValue = "WEEK") MetricsBucket bucket,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.throughput(id, bucket, from, to)));
  }

  /** Where the time went, per status - the query that locates a queue. */
  @GetMapping("/time-in-status")
  public ResponseEntity<ApiResponse<TimeInStatusResponse>> getTimeInStatus(
      @PathVariable Integer id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.timeInStatus(id, from, to)));
  }

  @GetMapping("/flow-efficiency")
  public ResponseEntity<ApiResponse<FlowEfficiencyResponse>> getFlowEfficiency(
      @PathVariable Integer id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.flowEfficiency(id, from, to)));
  }

  @GetMapping("/reopen-rate")
  public ResponseEntity<ApiResponse<ReopenRateResponse>> getReopenRate(
      @PathVariable Integer id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.reopenRate(id, from, to)));
  }

  /**
   * What is on the board right now and what has been stuck longest.
   *
   * <p>{@code asOf} rather than {@code from}/{@code to}, and it defaults to now - work in progress
   * is a level, not a flow. Passing a past instant asks what the board looked like then, which is
   * what makes a screenshot of this reproducible.
   */
  @GetMapping("/wip")
  public ResponseEntity<ApiResponse<WipResponse>> getWip(
      @PathVariable Integer id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime asOf) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.wip(id, asOf)));
  }

  /**
   * Work arriving against work leaving - whether the pile is growing, regardless of how fast it
   * moves.
   */
  @GetMapping("/net-flow")
  public ResponseEntity<ApiResponse<NetFlowResponse>> getNetFlow(
      @PathVariable Integer id,
      @RequestParam(defaultValue = "WEEK") MetricsBucket bucket,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.netFlow(id, bucket, from, to)));
  }

  /** Throughput split by type or priority - see {@link MetricsDimension} for why those two only. */
  @GetMapping("/throughput-breakdown")
  public ResponseEntity<ApiResponse<ThroughputBreakdownResponse>> getThroughputBreakdown(
      @PathVariable Integer id,
      @RequestParam(defaultValue = "TYPE") MetricsDimension dimension,
      @RequestParam(defaultValue = "WEEK") MetricsBucket bucket,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(
        ApiResponse.ok(issueMetricsService.throughputBreakdown(id, dimension, bucket, from, to)));
  }

  /** Bugs filed against everything filed, and bugs filed against work delivered. */
  @GetMapping("/defect-ratio")
  public ResponseEntity<ApiResponse<DefectRatioResponse>> getDefectRatio(
      @PathVariable Integer id,
      @RequestParam(defaultValue = "WEEK") MetricsBucket bucket,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.defectRatio(id, bucket, from, to)));
  }

  /** How long bugs take to fix, measured from when they were reported rather than picked up. */
  @GetMapping("/bug-mttr")
  public ResponseEntity<ApiResponse<DurationStatsResponse>> getBugMttr(
      @PathVariable Integer id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.bugMttr(id, from, to)));
  }

  /**
   * Committed against delivered, per sprint. No window: a sprint is its own, and the series is
   * every sprint the project has run.
   */
  @GetMapping("/velocity")
  public ResponseEntity<ApiResponse<VelocityResponse>> getVelocity(@PathVariable Integer id) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.velocity(id)));
  }

  /**
   * One sprint's burndown. The dates come from the sprint, so this takes no window either - only
   * the sprint to draw.
   */
  @GetMapping("/burndown")
  public ResponseEntity<ApiResponse<BurndownResponse>> getBurndown(
      @PathVariable Integer id, @RequestParam Integer sprintId) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.burndown(id, sprintId)));
  }

  /**
   * A cumulative flow diagram, bucketed by day because a coarser cut hides the queue it exists to
   * show.
   */
  @GetMapping("/cfd")
  public ResponseEntity<ApiResponse<CumulativeFlowResponse>> getCumulativeFlow(
      @PathVariable Integer id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    return ResponseEntity.ok(ApiResponse.ok(issueMetricsService.cumulativeFlow(id, from, to)));
  }
}
