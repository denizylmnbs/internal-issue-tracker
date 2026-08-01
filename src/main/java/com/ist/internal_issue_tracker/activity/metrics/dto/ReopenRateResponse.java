package com.ist.internal_issue_tracker.activity.metrics.dto;

/** What share of the work finished in the window came back - see {@code ReopenStats}. */
public record ReopenRateResponse(
    MetricWindow window, Long doneIssueCount, Long reopenedIssueCount, Double reopenRate) {}
