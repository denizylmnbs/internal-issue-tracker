import { ChartCard } from "./ChartCard";
import { formatDateOnly, formatDurationSeconds, formatPercent } from "@/lib/format";
import type { FlowEfficiencyResponse, ReopenRateResponse } from "@/lib/api/types";

export function FlowEfficiencyCard({
  data,
  isLoading,
}: {
  data: FlowEfficiencyResponse | undefined;
  isLoading: boolean;
}) {
  return (
    <ChartCard
      title="Flow efficiency"
      subtitle={data ? `${formatDateOnly(data.window.from)} – ${formatDateOnly(data.window.to)}` : undefined}
    >
      {isLoading || !data ? (
        <div className="h-16 animate-pulse rounded bg-secondary" />
      ) : !data.totalSeconds ? (
        <p className="text-sm text-slate">No status transitions in this window.</p>
      ) : (
        <div>
          <p className="font-data text-3xl font-semibold leading-none">
            {formatPercent(data.flowEfficiency)}
          </p>
          <p className="mt-1 text-xs text-slate">
            worked share of elapsed time — {formatDurationSeconds(data.activeSeconds)} active of{" "}
            {formatDurationSeconds(data.totalSeconds)} total
          </p>
        </div>
      )}
    </ChartCard>
  );
}

export function ReopenRateCard({
  data,
  isLoading,
}: {
  data: ReopenRateResponse | undefined;
  isLoading: boolean;
}) {
  return (
    <ChartCard
      title="Reopen rate"
      subtitle={data ? `${formatDateOnly(data.window.from)} – ${formatDateOnly(data.window.to)}` : undefined}
    >
      {isLoading || !data ? (
        <div className="h-16 animate-pulse rounded bg-secondary" />
      ) : data.doneIssueCount === 0 ? (
        <p className="text-sm text-slate">No issues finished in this window.</p>
      ) : (
        <div>
          <p className="font-data text-3xl font-semibold leading-none">{formatPercent(data.reopenRate)}</p>
          <p className="mt-1 text-xs text-slate">
            {data.reopenedIssueCount} of {data.doneIssueCount} finished issues came back
          </p>
        </div>
      )}
    </ChartCard>
  );
}
