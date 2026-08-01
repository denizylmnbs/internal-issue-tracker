package com.ist.internal_issue_tracker.activity.metrics.dto;

import java.time.OffsetDateTime;

/**
 * The window a metric was computed over, echoed back on every response.
 *
 * <p>Not redundant with the request: both bounds are optional and the service fills them in, so
 * without this the caller cannot tell what period the number describes. A metric without its window
 * is not a metric.
 *
 * <p>Half-open - {@code from} inclusive, {@code to} exclusive - so consecutive windows tile without
 * counting a boundary event twice.
 */
public record MetricWindow(OffsetDateTime from, OffsetDateTime to) {}
