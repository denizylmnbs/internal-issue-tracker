package com.ist.internal_issue_tracker.activity.metrics;

/**
 * The shape both cycle time and lead time come back in: a count and a distribution.
 *
 * <p>An interface projection rather than a record, for the reason given on {@code
 * ProjectParticipant} - the query is native, and Hibernate can only map a constructor expression
 * from HQL. Getter names map to the query's snake_case aliases.
 *
 * <p>Percentiles rather than an average alone. Cycle time is heavy-tailed - a handful of issues sit
 * in review for weeks - so the mean sits above most of the data and describes nothing anyone
 * experiences. The number a team can promise is p85. The average is returned alongside because its
 * distance from p50 is itself the signal that the tail is long.
 *
 * <p>Every field is null when no issue completed in the window. That is left as null rather than
 * zeroed: zero seconds and no data are different answers, and a chart that draws one as the other
 * is lying.
 */
public interface DurationStats {

  Long getIssueCount();

  Double getAvgSeconds();

  Double getP50Seconds();

  Double getP85Seconds();

  Double getP95Seconds();
}
