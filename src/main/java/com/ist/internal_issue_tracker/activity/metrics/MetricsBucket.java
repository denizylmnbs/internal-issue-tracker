package com.ist.internal_issue_tracker.activity.metrics;

/**
 * How a time series is grouped. The constant's {@link #unit()} is what reaches PostgreSQL's {@code
 * date_trunc}, as a bound parameter rather than as concatenated text - the enum is what makes that
 * safe, since no value outside these three can ever be passed.
 */
public enum MetricsBucket {
  DAY("day"),
  WEEK("week"),
  MONTH("month");

  private final String unit;

  MetricsBucket(String unit) {
    this.unit = unit;
  }

  public String unit() {
    return unit;
  }
}
