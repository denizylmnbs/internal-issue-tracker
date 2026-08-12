package com.ist.internal_issue_tracker.activity.metrics;

import com.ist.internal_issue_tracker.shared.event.FieldDefinitionsChangedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps every {@code metrics-*} cache in {@link IssueMetricsService} honest against changes to the
 * field definitions those metrics now read through {@code FieldDefinitionLookup} - a status gaining
 * or losing {@code isDone}/{@code isCancelled}/{@code isActiveWork}/{@code isDefect} would otherwise
 * go on returning a stale number until the cache's own TTL expired.
 *
 * <p>Every {@code metrics-*} method is {@code @Cacheable} with no explicit {@code key}, so its
 * default key is the full argument list - bucket, dimension, window, and so on, not just {@code
 * projectId}. That makes evicting only the entries for one project impossible without duplicating
 * the same argument shape here, so this clears each cache region wholesale on any field-definition
 * change, anywhere. Coarser than the per-key eviction {@code ProjectParticipantCacheEvictionListener}
 * does, but correct, and field definitions change rarely enough that the cost of a wider miss is
 * negligible next to the cost of a wrong number.
 */
@Component
@RequiredArgsConstructor
class MetricsCacheEvictionListener {

  private static final List<String> METRIC_CACHE_NAMES =
      List.of(
          "metrics-cycleTime",
          "metrics-leadTime",
          "metrics-throughput",
          "metrics-timeInStatus",
          "metrics-flowEfficiency",
          "metrics-reopenRate",
          "metrics-wip",
          "metrics-netFlow",
          "metrics-throughputBreakdown",
          "metrics-defectRatio",
          "metrics-bugMttr",
          "metrics-velocity",
          "metrics-burndown",
          "metrics-cumulativeFlow");

  private final CacheManager cacheManager;

  @EventListener
  void onFieldDefinitionsChanged(FieldDefinitionsChangedEvent event) {
    for (String cacheName : METRIC_CACHE_NAMES) {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
      }
    }
  }
}
