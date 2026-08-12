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
 * <p>The entity type is {@link IssueActivity} only because Spring Data needs one; nothing here
 * loads an entity.
 */
interface IssueMetricsRepository extends JpaRepository<IssueActivity, Integer> {

  /**
   * From first {@code IN_PROGRESS} to first {@code DONE} - how long work took once it was started.
   *
   * <p>{@code min} on both sides rather than {@code max}: an issue that was reopened and finished
   * again should report the time it first took, not the span across the whole round trip. The
   * reopen is a separate fact, and {@link #reopenStats} is where it is told.
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
   * Issues completed per bucket, keyed on the first time each reached {@code DONE} so that
   * reopening and re-finishing an issue does not let it be delivered twice.
   *
   * <p>{@code :bucket} is bound, never concatenated, and can only hold one of {@link
   * MetricsBucket}'s three units.
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
   * {@code Issue#status}. If that default ever changes, this line has to change with it - the two
   * are tied together by nothing the compiler can see.
   *
   * <p>An issue still sitting in a status has no next row, so its span runs to the end of the
   * window rather than being dropped: work that has been stuck for a month is the most important
   * thing this query has to say.
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
   * and named in {@link MetricStatus#ACTIVE}; the two are kept in step by {@code
   * MetricStatusCoverageTest}.
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
   * <p>{@code bool_or} collapses each issue to a yes or no, so a bug that bounced three times
   * counts once - see {@link ReopenStats}. The {@code HAVING} keeps issues that never reached
   * {@code DONE} out of the denominator: work still in progress has not had the chance to be
   * reopened.
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

  // ==========================================================================
  // Everything below reconstructs issue state from the log rather than reading
  // a duration out of it. Three patterns recur, and they are worth naming once.
  //
  // "LATEST WINS" is spelled `(array_agg(x ORDER BY created_at DESC, id DESC))[1]`
  // rather than as a window function or DISTINCT ON, because several of these
  // need the latest value of one column, the latest value of a *differently
  // filtered* column, and an aggregate over all of them, in one pass. The FILTER
  // clause composes with array_agg and not with DISTINCT ON. The id tie-break is
  // not decoration: one operation writes several rows sharing one timestamp.
  //
  // "STATUS COMES FROM TWO ACTION TYPES." An issue's status is the new_value of
  // its last STATUS_UPDATED, unless it has never had one, in which case it is
  // BACKLOG - the default on `Issue#status`. Rows of any other action type carry
  // an unrelated new_value and must be filtered out before the latest is taken,
  // or an issue whose priority changed most recently would report its status as
  // CRITICAL.
  //
  // "DELETED IS A ROW, NOT AN ABSENCE." Soft-deleted issues keep their history,
  // so every reconstruction has to exclude them explicitly. `bool_or` over the
  // whole issue is what does it, and it is evaluated as-of the same cut-off as
  // everything else, so an issue deleted in March is still present in February.
  // ==========================================================================

  /**
   * What is in flight right now, grouped by status, with how long it has been there.
   *
   * <p>A level rather than a flow, so it takes no window - see {@link WipStatusCount}. {@code asOf}
   * exists to make it reproducible, not to make it a range: passing last Tuesday asks what the
   * board looked like last Tuesday.
   */
  @Query(
      value =
          """
          WITH latest AS (
            SELECT a.issue_id,
                   (array_agg(CASE WHEN a.action_type = 'CREATED' THEN 'BACKLOG' ELSE a.new_value END
                              ORDER BY a.created_at DESC, a.id DESC)
                      FILTER (WHERE a.action_type IN ('CREATED', 'STATUS_UPDATED')))[1] AS status,
                   max(a.created_at) FILTER (WHERE a.action_type IN ('CREATED', 'STATUS_UPDATED'))
                                                                                        AS entered_at,
                   (array_agg(a.story_point ORDER BY a.created_at DESC, a.id DESC))[1]  AS story_point,
                   bool_or(a.action_type = 'DELETED')                                   AS deleted
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.created_at <= :asOf
             GROUP BY a.issue_id
          )
          SELECT status                                                      AS status,
                 count(*)                                                    AS issue_count,
                 CAST(coalesce(sum(coalesce(story_point, 0)), 0) AS bigint)   AS story_points,
                 CAST(EXTRACT(EPOCH FROM (:asOf - min(entered_at))) AS double precision)
                                                                             AS oldest_age_seconds,
                 CAST(percentile_cont(0.50) WITHIN GROUP (
                        ORDER BY EXTRACT(EPOCH FROM (:asOf - entered_at))) AS double precision)
                                                                             AS p50_age_seconds
            FROM latest
           WHERE NOT deleted
             AND status IS NOT NULL
             AND status NOT IN ('DONE', 'CANCELLED')
           GROUP BY status
           ORDER BY status
          """,
      nativeQuery = true)
  List<WipStatusCount> wipByStatus(
      @Param("projectId") Integer projectId, @Param("asOf") OffsetDateTime asOf);

  /**
   * The oldest in-flight issues, named. {@code BACKLOG} is excluded - see {@link AgingIssue} - and
   * so is anything finished, cancelled or deleted.
   */
  @Query(
      value =
          """
          WITH latest AS (
            SELECT a.issue_id,
                   (array_agg(CASE WHEN a.action_type = 'CREATED' THEN 'BACKLOG' ELSE a.new_value END
                              ORDER BY a.created_at DESC, a.id DESC)
                      FILTER (WHERE a.action_type IN ('CREATED', 'STATUS_UPDATED')))[1] AS status,
                   max(a.created_at) FILTER (WHERE a.action_type IN ('CREATED', 'STATUS_UPDATED'))
                                                                                        AS entered_at,
                   (array_agg(a.story_point ORDER BY a.created_at DESC, a.id DESC))[1]  AS story_point,
                   (array_agg(a.issue_type  ORDER BY a.created_at DESC, a.id DESC))[1]  AS issue_type,
                   (array_agg(a.priority    ORDER BY a.created_at DESC, a.id DESC))[1]  AS priority,
                   bool_or(a.action_type = 'DELETED')                                   AS deleted
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.created_at <= :asOf
             GROUP BY a.issue_id
          )
          SELECT issue_id                                                            AS issue_id,
                 status                                                              AS status,
                 entered_at                                                          AS entered_at,
                 CAST(EXTRACT(EPOCH FROM (:asOf - entered_at)) AS double precision)   AS age_seconds,
                 story_point                                                         AS story_point,
                 issue_type                                                          AS issue_type,
                 priority                                                            AS priority
            FROM latest
           WHERE NOT deleted
             AND status IS NOT NULL
             AND status NOT IN ('BACKLOG', 'DONE', 'CANCELLED')
           ORDER BY entered_at
           LIMIT :limit
          """,
      nativeQuery = true)
  List<AgingIssue> agingWip(
      @Param("projectId") Integer projectId,
      @Param("asOf") OffsetDateTime asOf,
      @Param("limit") int limit);

  /**
   * Arrivals against departures, per bucket, with a running total.
   *
   * <p>A {@code FULL OUTER JOIN} rather than an inner one, so a bucket where work only arrived and
   * a bucket where work only finished both survive. That is why the key is {@code coalesce(c.b,
   * d.b)} and why it has to be repeated in the window's {@code ORDER BY} - an output alias is not
   * visible there.
   */
  @Query(
      value =
          """
          WITH created AS (
            SELECT date_trunc(CAST(:bucket AS text), a.created_at) AS b, count(*) AS c
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.action_type = 'CREATED'
               AND a.created_at >= :from
               AND a.created_at <  :to
             GROUP BY 1
          ), completed AS (
            SELECT date_trunc(CAST(:bucket AS text), d.done_at) AS b, count(*) AS c
              FROM (SELECT a.issue_id, min(a.created_at) AS done_at
                      FROM issue_activities a
                     WHERE a.project_id = :projectId
                       AND a.action_type = 'STATUS_UPDATED'
                       AND a.new_value = 'DONE'
                     GROUP BY a.issue_id) d
             WHERE d.done_at >= :from AND d.done_at < :to
             GROUP BY 1
          )
          SELECT coalesce(cr.b, co.b)                              AS bucket_start,
                 coalesce(cr.c, 0)                                 AS created_count,
                 coalesce(co.c, 0)                                 AS completed_count,
                 coalesce(cr.c, 0) - coalesce(co.c, 0)             AS net_count,
                 CAST(sum(coalesce(cr.c, 0) - coalesce(co.c, 0))
                        OVER (ORDER BY coalesce(cr.b, co.b)) AS bigint)
                                                                   AS cumulative_net_count
            FROM created cr
            FULL OUTER JOIN completed co ON cr.b = co.b
           ORDER BY 1
          """,
      nativeQuery = true)
  List<NetFlowBucket> netFlow(
      @Param("projectId") Integer projectId,
      @Param("bucket") String bucket,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * Throughput cut by type or by priority, whichever {@code :dimension} names.
   *
   * <p>The branch is on a bound value inside a {@code CASE}, never on an interpolated column name -
   * see {@link MetricsDimension}. {@code DISTINCT ON} takes each issue's first {@code DONE} along
   * with the dimensions frozen on that row.
   */
  @Query(
      value =
          """
          WITH done AS (
            SELECT DISTINCT ON (a.issue_id)
                   a.issue_id, a.created_at AS done_at, a.issue_type, a.priority, a.story_point
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.action_type = 'STATUS_UPDATED'
               AND a.new_value = 'DONE'
             ORDER BY a.issue_id, a.created_at, a.id
          )
          SELECT date_trunc(CAST(:bucket AS text), done_at)                        AS bucket_start,
                 coalesce(CASE WHEN CAST(:dimension AS text) = 'TYPE' THEN issue_type
                               ELSE priority END, 'UNSET')                         AS dimension_value,
                 count(*)                                                          AS completed_count,
                 CAST(coalesce(sum(coalesce(story_point, 0)), 0) AS bigint)         AS completed_points
            FROM done
           WHERE done_at >= :from AND done_at < :to
           GROUP BY 1, 2
           ORDER BY 1, 2
          """,
      nativeQuery = true)
  List<BreakdownBucket> throughputBreakdown(
      @Param("projectId") Integer projectId,
      @Param("dimension") String dimension,
      @Param("bucket") String bucket,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * Bug share and defect density in one pass - see {@link DefectBucket} for why they are different
   * questions and why both denominators are returned.
   *
   * <p>Note which side each count comes from: bugs are counted where they were filed, delivery
   * where it was delivered, and the density divides one by the other across the same bucket.
   */
  @Query(
      value =
          """
          WITH created AS (
            SELECT date_trunc(CAST(:bucket AS text), a.created_at) AS b,
                   count(*)                                        AS total,
                   count(*) FILTER (WHERE a.issue_type = 'BUG')    AS bugs
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.action_type = 'CREATED'
               AND a.created_at >= :from
               AND a.created_at <  :to
             GROUP BY 1
          ), done AS (
            SELECT DISTINCT ON (a.issue_id)
                   a.issue_id, a.created_at AS done_at, a.issue_type, a.story_point
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.action_type = 'STATUS_UPDATED'
               AND a.new_value = 'DONE'
             ORDER BY a.issue_id, a.created_at, a.id
          ), completed AS (
            SELECT date_trunc(CAST(:bucket AS text), done_at)  AS b,
                   count(*)                                    AS total,
                   count(*) FILTER (WHERE issue_type = 'BUG')  AS bugs,
                   sum(coalesce(story_point, 0))               AS points
              FROM done
             WHERE done_at >= :from AND done_at < :to
             GROUP BY 1
          )
          SELECT coalesce(c.b, d.b)                                          AS bucket_start,
                 coalesce(c.total, 0)                                        AS created_count,
                 coalesce(c.bugs, 0)                                         AS created_bug_count,
                 CAST(coalesce(c.bugs, 0) AS double precision)
                   / NULLIF(coalesce(c.total, 0), 0)                         AS created_bug_share,
                 coalesce(d.total, 0)                                        AS completed_count,
                 coalesce(d.bugs, 0)                                         AS completed_bug_count,
                 CAST(coalesce(d.points, 0) AS bigint)                       AS completed_story_points,
                 CAST(coalesce(c.bugs, 0) AS double precision)
                   / NULLIF(coalesce(d.total, 0), 0)                         AS defects_per_completed_issue,
                 CAST(coalesce(c.bugs, 0) AS double precision)
                   / NULLIF(coalesce(d.points, 0), 0)                        AS defects_per_completed_point
            FROM created c
            FULL OUTER JOIN completed d ON c.b = d.b
           ORDER BY 1
          """,
      nativeQuery = true)
  List<DefectBucket> defectStats(
      @Param("projectId") Integer projectId,
      @Param("bucket") String bucket,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * Mean time to resolve a bug: filed to first {@code DONE}, for issues that were bugs when they
   * were resolved.
   *
   * <p>Lead time rather than cycle time, deliberately. The clock a defect is judged by starts when
   * it is reported, not when someone gets round to it - the time it spent waiting in the queue is
   * most of what MTTR is meant to expose.
   *
   * <p>The type is read from the {@code DONE} row rather than the {@code CREATED} one, so an issue
   * filed as a task and reclassified as a bug on investigation counts as the bug it turned out to
   * be. The counterpart in {@link #defectStats} reads it from {@code CREATED}, and the difference
   * is intentional: that metric asks how many defects a period produced, this one asks how long
   * defects take to fix.
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
            SELECT DISTINCT ON (a.issue_id) a.issue_id, a.created_at AS done_at, a.issue_type
              FROM issue_activities a
             WHERE a.project_id = :projectId
               AND a.action_type = 'STATUS_UPDATED'
               AND a.new_value = 'DONE'
             ORDER BY a.issue_id, a.created_at, a.id
          ), spans AS (
            SELECT CAST(EXTRACT(EPOCH FROM (d.done_at - c.created_at)) AS double precision) AS secs
              FROM created c
              JOIN done d ON d.issue_id = c.issue_id
             WHERE d.issue_type = 'BUG'
               AND d.done_at > c.created_at
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
  DurationStats bugMttr(
      @Param("projectId") Integer projectId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * Points and issues delivered, per sprint, over the project's whole history.
   *
   * <p>Unwindowed on purpose: velocity is read as a sequence of sprints, and clipping it to ninety
   * days would cut a sprint in half rather than leaving it out. The service pairs each row with the
   * sprint's commitment - see {@link SprintVelocity}.
   */
  @Query(
      value =
          """
          SELECT d.sprint_id                                                  AS sprint_id,
                 count(*)                                                     AS completed_issue_count,
                 CAST(coalesce(sum(coalesce(d.story_point, 0)), 0) AS bigint) AS completed_points
            FROM (SELECT DISTINCT ON (a.issue_id) a.issue_id, a.sprint_id, a.story_point
                    FROM issue_activities a
                   WHERE a.project_id = :projectId
                     AND a.action_type = 'STATUS_UPDATED'
                     AND a.new_value = 'DONE'
                   ORDER BY a.issue_id, a.created_at, a.id) d
           WHERE d.sprint_id IS NOT NULL
           GROUP BY d.sprint_id
          """,
      nativeQuery = true)
  List<SprintVelocity> velocity(@Param("projectId") Integer projectId);

  /**
   * A sprint burndown, one row per day, reconstructed by replaying the log up to the end of each
   * day.
   *
   * <p>{@code generate_series} supplies the calendar - the log has rows only on days something
   * happened, and a burndown with gaps in it is unreadable. Each day then re-aggregates the
   * project's history up to that point, which is why this is the most expensive query here and why
   * it is bounded to one sprint's dates rather than to a ninety-day default.
   *
   * <p>The cut-off is {@code < day + 1 day} rather than {@code <= day}, because {@code day} is
   * midnight: the point is the state at the <em>end</em> of that day.
   */
  @Query(
      value =
          """
          WITH days AS (
            SELECT generate_series(date_trunc('day', CAST(:from AS timestamptz)),
                                   date_trunc('day', CAST(:to   AS timestamptz)),
                                   interval '1 day') AS day
          ), snap AS (
            SELECT d.day, s.*
              FROM days d
              CROSS JOIN LATERAL (
                SELECT a.issue_id,
                       (array_agg(a.sprint_id   ORDER BY a.created_at DESC, a.id DESC))[1] AS sprint_id,
                       (array_agg(a.story_point ORDER BY a.created_at DESC, a.id DESC))[1] AS story_point,
                       (array_agg(CASE WHEN a.action_type = 'CREATED' THEN 'BACKLOG' ELSE a.new_value END
                                  ORDER BY a.created_at DESC, a.id DESC)
                          FILTER (WHERE a.action_type IN ('CREATED', 'STATUS_UPDATED')))[1] AS status,
                       bool_or(a.action_type = 'DELETED')                                   AS deleted
                  FROM issue_activities a
                 WHERE a.project_id = :projectId
                   AND a.created_at < d.day + interval '1 day'
                 GROUP BY a.issue_id
              ) s
          )
          SELECT day                                                                AS bucket_start,
                 CAST(coalesce(sum(coalesce(story_point, 0))
                        FILTER (WHERE status <> 'DONE'), 0) AS bigint)              AS remaining_points,
                 count(*) FILTER (WHERE status <> 'DONE')                           AS remaining_issue_count,
                 CAST(coalesce(sum(coalesce(story_point, 0))
                        FILTER (WHERE status = 'DONE'), 0) AS bigint)               AS completed_points,
                 CAST(coalesce(sum(coalesce(story_point, 0)), 0) AS bigint)         AS scope_points
            FROM snap
           WHERE sprint_id = :sprintId
             AND NOT deleted
             AND status IS NOT NULL
             AND status <> 'CANCELLED'
           GROUP BY day
           ORDER BY day
          """,
      nativeQuery = true)
  List<BurndownPoint> burndown(
      @Param("projectId") Integer projectId,
      @Param("sprintId") Integer sprintId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  /**
   * A cumulative flow diagram: how many issues stood in each status at the end of each day.
   *
   * <p>The same daily replay as {@link #burndown}, without the sprint filter and counting heads
   * rather than points. Rows are emitted only for statuses that were occupied - see {@link
   * CfdPoint}.
   */
  @Query(
      value =
          """
          WITH days AS (
            SELECT generate_series(date_trunc('day', CAST(:from AS timestamptz)),
                                   date_trunc('day', CAST(:to   AS timestamptz)),
                                   interval '1 day') AS day
          ), snap AS (
            SELECT d.day, s.*
              FROM days d
              CROSS JOIN LATERAL (
                SELECT a.issue_id,
                       (array_agg(CASE WHEN a.action_type = 'CREATED' THEN 'BACKLOG' ELSE a.new_value END
                                  ORDER BY a.created_at DESC, a.id DESC)
                          FILTER (WHERE a.action_type IN ('CREATED', 'STATUS_UPDATED')))[1] AS status,
                       bool_or(a.action_type = 'DELETED')                                   AS deleted
                  FROM issue_activities a
                 WHERE a.project_id = :projectId
                   AND a.created_at < d.day + interval '1 day'
                 GROUP BY a.issue_id
              ) s
          )
          SELECT day      AS bucket_start,
                 status   AS status,
                 count(*) AS issue_count
            FROM snap
           WHERE NOT deleted
             AND status IS NOT NULL
           GROUP BY day, status
           ORDER BY day, status
          """,
      nativeQuery = true)
  List<CfdPoint> cumulativeFlow(
      @Param("projectId") Integer projectId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);
}
