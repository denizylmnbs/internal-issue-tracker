package com.ist.internal_issue_tracker.shared.event;

/**
 * Published whenever a project's (or, for a global kind, the instance's) field definitions
 * change - a status added, relabelled, reflagged, reordered or retired.
 *
 * <p>{@code projectId} is null for a change to a global kind ({@code PROJECT_STATUS}, {@code
 * TEAM_FIELD}). {@code activity.metrics}' {@code MetricsCacheEvictionListener} is the only
 * consumer today: every {@code IssueMetricsService} method is {@code @Cacheable} keyed on its
 * arguments, so a status gaining or losing {@code isDone}/{@code isCancelled}/{@code
 * isActiveWork}/{@code isDefect} would otherwise go on returning a stale number until the cache's
 * TTL expired.
 */
public record FieldDefinitionsChangedEvent(Integer projectId) {}
