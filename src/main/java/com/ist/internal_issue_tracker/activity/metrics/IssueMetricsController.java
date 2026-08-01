package com.ist.internal_issue_tracker.activity.metrics;

import com.ist.internal_issue_tracker.activity.metrics.dto.DurationStatsResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.FlowEfficiencyResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.ReopenRateResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.ThroughputResponse;
import com.ist.internal_issue_tracker.activity.metrics.dto.TimeInStatusResponse;
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
}
