import { format, formatDistanceToNow } from "date-fns";

/** Backend timestamps are UTC OffsetDateTime; `new Date()` parses the offset
 * and `format` below renders in the browser's local zone automatically.
 *
 * Some "joined"/"assigned" timestamps are derived server-side from a
 * nullable `updated_at` column (a row that was never touched after being
 * inserted by hand has no value there) — every formatter below accepts
 * null/undefined/invalid input and renders "—" rather than epoch. */

const EM_DASH = "—";

function safeFormat(iso: string | null | undefined, pattern: string): string {
  if (!iso) return EM_DASH;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return EM_DASH;
  return format(date, pattern);
}

export const formatTimestamp = (iso: string | null | undefined) => safeFormat(iso, "MMM d, HH:mm");
export const formatDateOnly = (iso: string | null | undefined) => safeFormat(iso, "MMM d, yyyy");
export const formatClock = (iso: string | null | undefined) => safeFormat(iso, "HH:mm");
export const formatRelative = (iso: string | null | undefined) => {
  if (!iso) return EM_DASH;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return EM_DASH;
  return formatDistanceToNow(date, { addSuffix: true });
};

/** All metric durations are seconds (docs/API.md §4.13) — the client picks
 * the presentation. Renders `—` for null so "no data" reads differently from
 * "instant". */
export function formatDurationSeconds(seconds: number | null | undefined): string {
  if (seconds == null) return "—";
  const abs = Math.abs(seconds);
  if (abs < 60) return `${Math.round(seconds)}s`;
  const minutes = seconds / 60;
  if (abs < 3600) return `${Math.round(minutes)}m`;
  const hours = seconds / 3600;
  if (abs < 86400) return `${round1(hours)}h`;
  const days = seconds / 86400;
  return `${round1(days)}d`;
}

const round1 = (n: number) => Math.round(n * 10) / 10;

export function formatPercent(fraction: number | null | undefined): string {
  if (fraction == null) return "—";
  return `${Math.round(fraction * 100)}%`;
}

export function formatRatio(ratio: number | null | undefined): string {
  if (ratio == null) return "—";
  return ratio.toFixed(2);
}
