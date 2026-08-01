package com.ist.internal_issue_tracker.activity.metrics.dto;

/**
 * Cycle time or lead time over a window. Durations are seconds, left for the client to render - a
 * server that decides between "3 days" and "76 hours" has made a presentation choice on its behalf.
 *
 * <p>Every duration is null when {@code issueCount} is zero. See {@code DurationStats} for why that
 * is not zeroed, and why p85 is the number worth quoting.
 */
public record DurationStatsResponse(
    MetricWindow window,
    Long issueCount,
    Double avgSeconds,
    Double p50Seconds,
    Double p85Seconds,
    Double p95Seconds) {}
