package com.ist.internal_issue_tracker.activity.metrics;

import java.time.Instant;

/**
 * One point of the throughput series: how many issues were completed in one bucket.
 *
 * <p>Throughput is velocity's plainer sibling - it counts issues rather than story points, so it
 * cannot be moved by re-estimating, and it needs no agreement on what a point is worth.
 *
 * <p>Empty buckets are absent rather than zero. Filling the gaps needs a calendar the database does
 * not have to generate here, and a client drawing a series knows its own axis.
 *
 * <p>{@code Instant} rather than {@code OffsetDateTime}, because that is what the driver hands back
 * for the {@code timestamptz} {@code date_trunc} returns - an interface projection performs no
 * conversion of its own and fails outright on a mismatch. The service is where it becomes an offset
 * time, and the offset it is given is UTC.
 *
 * <p>A subtlety worth knowing, and one that bit: {@code date_trunc} cuts a {@code timestamptz} in
 * the <em>session's</em> time zone, which the driver sets from the JVM's default unless told
 * otherwise. On a machine at +03 that put every bucket three hours before the UTC boundary, so a
 * month's data came back stamped with the previous month - correct arithmetic, wrong label, and
 * wrong by a whole bucket rather than by three hours. {@code
 * spring.datasource.hikari.connection-init-sql} now pins the session to UTC so the cut and this
 * rendering agree, and so two deployments cannot bucket the same data differently.
 */
public interface ThroughputBucket {

  Instant getBucketStart();

  Long getCompletedCount();
}
