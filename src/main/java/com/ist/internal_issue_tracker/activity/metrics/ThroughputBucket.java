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
 * <p>A subtlety worth knowing: {@code date_trunc} cuts a {@code timestamptz} in the session's time
 * zone, so where a week begins depends on the database's {@code TimeZone} setting rather than on
 * anything this code says. It matters only at the boundary, but it is the reason two deployments can
 * bucket the same data differently.
 */
public interface ThroughputBucket {

  Instant getBucketStart();

  Long getCompletedCount();
}
