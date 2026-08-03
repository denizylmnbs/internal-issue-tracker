import { ChartCard } from "./ChartCard";
import { formatDateOnly, formatDurationSeconds } from "@/lib/format";
import type { DurationStatsResponse } from "@/lib/api/types";

/** p85 is the number worth quoting — the average hides the tail people
 * actually feel (docs/API.md §4.13). Given visual primacy accordingly. */
export function DurationCard({
  title,
  data,
  isLoading,
}: {
  title: string;
  data: DurationStatsResponse | undefined;
  isLoading: boolean;
}) {
  return (
    <ChartCard
      title={title}
      subtitle={
        data ? `${formatDateOnly(data.window.from)} – ${formatDateOnly(data.window.to)}` : undefined
      }
    >
      {isLoading || !data ? (
        <div className="h-16 animate-pulse rounded bg-secondary" />
      ) : data.issueCount === 0 ? (
        <p className="text-sm text-slate">No completed work in this window.</p>
      ) : (
        <div className="flex items-end gap-6">
          <div>
            <p className="font-data text-3xl font-semibold leading-none">
              {formatDurationSeconds(data.p85Seconds)}
            </p>
            <p className="mt-1 text-xs text-slate">p85 · {data.issueCount} issues</p>
          </div>
          <div className="flex gap-4 pb-0.5 text-xs text-slate">
            <span>
              p50 <span className="font-data text-ink">{formatDurationSeconds(data.p50Seconds)}</span>
            </span>
            <span>
              p95 <span className="font-data text-ink">{formatDurationSeconds(data.p95Seconds)}</span>
            </span>
            <span>
              avg <span className="font-data text-ink">{formatDurationSeconds(data.avgSeconds)}</span>
            </span>
          </div>
        </div>
      )}
    </ChartCard>
  );
}
