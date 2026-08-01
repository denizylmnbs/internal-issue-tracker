package com.ist.internal_issue_tracker.activity.metrics;

import com.ist.internal_issue_tracker.activity.IssueActivity;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every query here is native, and none of them could be anything else: {@code percentile_cont ...
 * WITHIN GROUP}, aggregate {@code FILTER}, {@code lead() OVER} and {@code date_trunc} have no JPQL
 * equivalent. They all read {@code issue_activities}, which this module owns, so nothing here
 * reaches across a module boundary.
 *
 * <p><b>Three things these queries are careful about.</b>
 *
 * <p><em>Casts are spelled {@code CAST(x AS double precision)} rather than {@code x::double
 * precision}.</em> Hibernate reads {@code :} as the start of a named parameter, so the shorthand
 * turns into a parameter named {@code double} and the query fails to bind. It is also why {@code
 * EXTRACT(EPOCH FROM interval)} is wrapped at all - PostgreSQL returns {@code numeric} from it,
 * which would surface as {@code BigDecimal} against a {@code Double} getter.
 *
 * <p><em>Windows order by {@code created_at, id}.</em> One operation writes several rows carrying
 * one instant, so ordering on the timestamp alone leaves {@code lead()} free to pair rows
 * arbitrarily - and a pairing that runs backwards produces a negative span that quietly drags an
 * average down.
 *
 * <p><em>The window filters the finish, not the start.</em> Applying it inside the CTE that finds
 * each issue's boundaries would drop any issue that started before the window and finished inside
 * it, which is precisely the long-running work a cycle time is meant to expose. "Completed in this
 * window" is the standard definition and the honest one.
 *
 * <p>The entity type is {@link IssueActivity} only because Spring Data needs one; nothing here loads
 * an entity.
 */
interface IssueMetricsRepository extends JpaRepository<IssueActivity, Integer> {

  /**
   * From first {@code IN_PROGRESS} to first {@code DONE} - how long work took once it was started.
   *
   * <p>{@code min} on both sides rather than {@code max}: an issue that was reopened and finished
   * again should report the time it first took, not the span across the whole round trip. The reopen
   * is a separate fact, and {@link #reopenStats} is where it is told.
   */
  @Query(
      value =
          """
          WITH bounds AS (
            SELECT a.issue_id,
                   min(a.created_at) FILTER (WHERE a.new_value = 'IN_PROGRESS') AS started_at,
                   min(a.created_at) FILTER (WHERE a.new_value = 'DONE')        AS done_at
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.action_type = 'STATUS_UPDATED'
             GROUP BY a.issue_id
          ), spans AS (
            SELECT CAST(EXTRACT(EPOCH FROM (done_at - started_at)) AS double precision) AS secs
              FROM bounds
             WHERE started_at IS NOT NULL
               AND done_at IS NOT NULL
               AND done_at > started_at
               AND done_at >= :from
               AND done_at <  :to
          )
          SELECT count(*)                                                                  AS issue_count,
                 CAST(avg(secs) AS double precision)                                       AS avg_seconds,
                 CAST(percentile_cont(0.50) WITHIN GROUP (ORDER BY secs) AS double precision) AS p50_seconds,
                 CAST(percentile_cont(0.85) WITHIN GROUP (ORDER BY secs) AS double precision) AS p85_seconds,
                 CAST(percentile_cont(0.95) WITHIN GROUP (ORDER BY secs) AS double precision) AS p95_seconds
            FROM spans
          """,
      nativeQuery = true)
  DurationStats cycleTime(
      @Param("projectId") Integer projectId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * From {@code CREATED} to first {@code DONE} - how long work took from being asked for, which is
   * the number the people waiting on it actually feel.
   *
   * <p>It is the metric the {@code CREATED} backfill in {@code V2} exists for: without those rows
   * every issue that predates the activity log would be missing its start and would drop out here
   * without a trace.
   */
  @Query(
      value =
          """
          WITH created AS (
            SELECT a.issue_id, min(a.created_at) AS created_at
              FROM issue_activities a
             WHERE a.project_id = :projectId AND a.action_type = 'CREATED'
             GROUP BY a.issue_id
          ), done AS (
            SELECT a.issue_id, min(a.created_at) AS done_at
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.action_type = 'STATUS_UPDATED'
               AND a.new_value = 'DONE'
             GROUP BY a.issue_id
          ), spans AS (
            SELECT CAST(EXTRACT(EPOCH FROM (d.done_at - c.created_at)) AS double precision) AS secs
              FROM created c
              JOIN done d ON d.issue_id = c.issue_id
             WHERE d.done_at > c.created_at
               AND d.done_at >= :from
               AND d.done_at <  :to
          )
          SELECT count(*)                                                                  AS issue_count,
                 CAST(avg(secs) AS double precision)                                       AS avg_seconds,
                 CAST(percentile_cont(0.50) WITHIN GROUP (ORDER BY secs) AS double precision) AS p50_seconds,
                 CAST(percentile_cont(0.85) WITHIN GROUP (ORDER BY secs) AS double precision) AS p85_seconds,
                 CAST(percentile_cont(0.95) WITHIN GROUP (ORDER BY secs) AS double precision) AS p95_seconds
            FROM spans
          """,
      nativeQuery = true)
  DurationStats leadTime(
      @Param("projectId") Integer projectId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * Issues completed per bucket, keyed on the first time each reached {@code DONE} so that reopening
   * and re-finishing an issue does not let it be delivered twice.
   *
   * <p>{@code :bucket} is bound, never concatenated, and can only hold one of {@link MetricsBucket}'s
   * three units.
   */
  @Query(
      value =
          """
          SELECT date_trunc(CAST(:bucket AS text), d.done_at) AS bucket_start,
                 count(*)                                     AS completed_count
            FROM (SELECT a.issue_id, min(a.created_at) AS done_at
                    FROM issue_activities a
                   WHERE a.project_id = :projectId
                     AND a.action_type = 'STATUS_UPDATED'
                     AND a.new_value = 'DONE'
                   GROUP BY a.issue_id) d
           WHERE d.done_at >= :from AND d.done_at < :to
           GROUP BY 1
           ORDER BY 1
          """,
      nativeQuery = true)
  List<ThroughputBucket> throughput(
      @Param("projectId") Integer projectId,
      @Param("bucket") String bucket,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * How long issues sat in each status, clipped to the window at both ends so a span that straddles
   * the boundary contributes only the part inside it.
   *
   * <p>The {@code CREATED} row is read as entering {@code BACKLOG}, which mirrors the default on
   * {@code Issue#status}. If that default ever changes, this line has to change with it - the two are
   * tied together by nothing the compiler can see.
   *
   * <p>An issue still sitting in a status has no next row, so its span runs to the end of the window
   * rather than being dropped: work that has been stuck for a month is the most important thing this
   * query has to say.
   */
  @Query(
      value =
          """
          WITH timeline AS (
            SELECT a.issue_id,
                   CASE WHEN a.action_type = 'CREATED' THEN 'BACKLOG' ELSE a.new_value END AS status,
                   a.created_at AS entered_at,
                   lead(a.created_at) OVER (PARTITION BY a.issue_id ORDER BY a.created_at, a.id) AS left_at
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.action_type IN ('CREATED', 'STATUS_UPDATED')
          ), spans AS (
            SELECT status,
                   issue_id,
                   CAST(EXTRACT(EPOCH FROM (
                     LEAST(COALESCE(left_at, :to), :to) - GREATEST(entered_at, :from)
                   )) AS double precision) AS secs
              FROM timeline
             WHERE entered_at < :to
               AND COALESCE(left_at, :to) > :from
          )
          SELECT status                                                                    AS status,
                 count(DISTINCT issue_id)                                                  AS issue_count,
                 CAST(sum(secs) AS double precision)                                       AS total_seconds,
                 CAST(percentile_cont(0.50) WITHIN GROUP (ORDER BY secs) AS double precision) AS p50_seconds
            FROM spans
           GROUP BY status
           ORDER BY status
          """,
      nativeQuery = true)
  List<StatusDuration> timeInStatus(
      @Param("projectId") Integer projectId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * The same timeline as {@link #timeInStatus}, reduced to one ratio: time in the active statuses
   * over time in all of them.
   *
   * <p>{@code NULLIF} guards the division, so an empty window yields null rather than a divide-by-
   * zero or a fabricated 0.0 - see {@link FlowEfficiencyStats}. The active set is spelled out here
   * and named in {@link MetricStatus#ACTIVE}; the two are kept in step by
   * {@code MetricStatusCoverageTest}.
   */
  @Query(
      value =
          """
          WITH timeline AS (
            SELECT a.issue_id,
                   CASE WHEN a.action_type = 'CREATED' THEN 'BACKLOG' ELSE a.new_value END AS status,
                   a.created_at AS entered_at,
                   lead(a.created_at) OVER (PARTITION BY a.issue_id ORDER BY a.created_at, a.id) AS left_at
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.action_type IN ('CREATED', 'STATUS_UPDATED')
          ), spans AS (
            SELECT status,
                   CAST(EXTRACT(EPOCH FROM (
                     LEAST(COALESCE(left_at, :to), :to) - GREATEST(entered_at, :from)
                   )) AS double precision) AS secs
              FROM timeline
             WHERE entered_at < :to
               AND COALESCE(left_at, :to) > :from
          )
          SELECT CAST(sum(secs) FILTER (WHERE status IN ('IN_PROGRESS', 'IN_REVIEW'))
                        / NULLIF(sum(secs), 0) AS double precision)                     AS flow_efficiency,
                 CAST(sum(secs) FILTER (WHERE status IN ('IN_PROGRESS', 'IN_REVIEW'))
                        AS double precision)                                            AS active_seconds,
                 CAST(sum(secs) AS double precision)                                    AS total_seconds
            FROM spans
          """,
      nativeQuery = true)
  FlowEfficiencyStats flowEfficiency(
      @Param("projectId") Integer projectId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * What share of the work finished in the window came back afterwards.
   *
   * <p>{@code bool_or} collapses each issue to a yes or no, so a bug that bounced three times counts
   * once - see {@link ReopenStats}. The {@code HAVING} keeps issues that never reached {@code DONE}
   * out of the denominator: work still in progress has not had the chance to be reopened.
   */
  @Query(
      value =
          """
          SELECT count(*)                                                     AS done_issue_count,
                 count(*) FILTER (WHERE reopened)                             AS reopened_issue_count,
                 CAST(count(*) FILTER (WHERE reopened) AS double precision)
                   / NULLIF(count(*), 0)                                      AS reopen_rate
            FROM (SELECT a.issue_id,
                         bool_or(a.old_value = 'DONE' AND a.new_value <> 'DONE') AS reopened
                    FROM issue_activities a
                   WHERE a.project_id = :projectId
                     AND a.action_type = 'STATUS_UPDATED'
                     AND a.created_at >= :from
                     AND a.created_at <  :to
                   GROUP BY a.issue_id
                  HAVING bool_or(a.new_value = 'DONE')) x
          """,
      nativeQuery = true)
  ReopenStats reopenStats(
      @Param("projectId") Integer projectId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);
}
