package com.ist.internal_issue_tracker.activity.metrics.dto;

/**
 * The worked share of elapsed time, as a fraction between 0 and 1, with the two totals it was
 * derived from so the ratio can be checked rather than taken on trust.
 */
public record FlowEfficiencyResponse(
    MetricWindow window, Double flowEfficiency, Double activeSeconds, Double totalSeconds) {}
