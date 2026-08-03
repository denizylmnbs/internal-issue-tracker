import type { MetricsBucket } from "@/lib/api/enums";

/**
 * docs/API.md §4.13: metric series are sparse — "empty buckets are absent,
 * not zero" — so every chart supplies its own zero baseline before drawing.
 * Generates every bucket boundary in [from, to) for the given cadence, then
 * merges in whatever points the server actually returned.
 *
 * The boundaries have to land on the *same* instants the server's
 * `date_trunc(bucket, ...)` produced (see `IssueMetricsRepository` / the
 * `ThroughputBucket` javadoc: the DB session is pinned to UTC), otherwise no
 * point ever lines up with a generated bucket and every series densifies to
 * all zeros. So `from` is first snapped down to a UTC bucket boundary before
 * stepping forward — DAY → UTC midnight, WEEK → UTC Monday midnight (Postgres
 * `date_trunc('week', ...)` is ISO/Monday-based, not date-fns' Sunday
 * default), MONTH → the 1st at UTC midnight.
 */
export function bucketStarts(from: string, to: string, bucket: MetricsBucket): string[] {
  const end = new Date(to).getTime();
  const starts: string[] = [];
  let cursor = truncateToUtcBucket(new Date(from), bucket);
  while (cursor.getTime() < end) {
    starts.push(cursor.toISOString());
    cursor = stepBucket(cursor, bucket);
  }
  return starts;
}

function truncateToUtcBucket(date: Date, bucket: MetricsBucket): Date {
  if (bucket === "DAY") {
    return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  }
  if (bucket === "MONTH") {
    return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), 1));
  }
  // WEEK: Postgres date_trunc('week', ...) truncates to Monday.
  const day = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const isoDow = day.getUTCDay() === 0 ? 7 : day.getUTCDay(); // Mon=1 ... Sun=7
  day.setUTCDate(day.getUTCDate() - (isoDow - 1));
  return day;
}

function stepBucket(date: Date, bucket: MetricsBucket): Date {
  if (bucket === "DAY") {
    return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate() + 1));
  }
  if (bucket === "WEEK") {
    return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate() + 7));
  }
  return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + 1, 1));
}

/** Normalizes an ISO timestamp to the epoch millisecond it names, so points
 * from the server (`2026-05-04T00:00:00Z`) and boundaries generated above
 * compare equal regardless of offset spelling or millisecond precision. */
export function bucketKey(iso: string): number {
  return new Date(iso).getTime();
}

/**
 * Merges a sparse `points` array (each with a `bucketStart`) onto the full
 * set of bucket boundaries, filling gaps with `zero`.
 */
export function densify<T extends { bucketStart: string }>(
  points: T[],
  from: string,
  to: string,
  bucket: MetricsBucket,
  zero: (bucketStart: string) => T,
): T[] {
  const byKey = new Map(points.map((p) => [bucketKey(p.bucketStart), p]));
  return bucketStarts(from, to, bucket).map((start) => byKey.get(bucketKey(start)) ?? zero(start));
}

/** Default window: the last 90 days ending now — mirrors the backend default
 * so the client's initial render matches what an omitted `from`/`to` would
 * have produced anyway. */
export function defaultWindow(): { from: string; to: string } {
  const to = new Date();
  const from = new Date(to);
  from.setDate(from.getDate() - 90);
  return { from: from.toISOString(), to: to.toISOString() };
}
