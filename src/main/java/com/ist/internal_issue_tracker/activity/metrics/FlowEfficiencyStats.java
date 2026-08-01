package com.ist.internal_issue_tracker.activity.metrics;

/**
 * The share of elapsed time that was actually worked, as a fraction between 0 and 1.
 *
 * <p>Healthy teams land somewhere between 0.15 and 0.40, which surprises people the first time they
 * measure it. A number near 0.05 does not mean anyone is idle - it means work sits in queues, and
 * the fix is in the process rather than in the people.
 *
 * <p>Null rather than zero when the window holds no time at all: "nothing happened" and "nothing was
 * worked on" are different claims, and only the second is an indictment.
 */
public interface FlowEfficiencyStats {

  Double getFlowEfficiency();

  Double getActiveSeconds();

  Double getTotalSeconds();
}
