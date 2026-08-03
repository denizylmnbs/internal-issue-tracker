"use client";

import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";
import { ChartCard } from "./ChartCard";
import { CHART, TOOLTIP_STYLE, AXIS_STYLE } from "./colors";
import { formatDurationSeconds } from "@/lib/format";
import { ISSUE_STATUS_LABEL } from "@/lib/api/enums";
import type { TimeInStatusResponse } from "@/lib/api/types";

/** Read across the entries: a large total against a small count is where a
 * queue is forming (docs/API.md §4.13). Horizontal bars, sorted by total, so
 * the queue is the first thing seen. */
export function TimeInStatusChart({
  data,
  isLoading,
}: {
  data: TimeInStatusResponse | undefined;
  isLoading: boolean;
}) {
  const rows = [...(data?.entries ?? [])]
    .sort((a, b) => b.totalSeconds - a.totalSeconds)
    .map((e) => ({ ...e, label: ISSUE_STATUS_LABEL[e.status] }));

  return (
    <ChartCard title="Time in status" subtitle="where the time went">
      {isLoading ? (
        <div className="h-48 animate-pulse rounded bg-secondary" />
      ) : rows.length === 0 ? (
        <p className="text-sm text-slate">No status transitions in this window.</p>
      ) : (
        <ResponsiveContainer width="100%" height={Math.max(140, rows.length * 34)}>
          <BarChart data={rows} layout="vertical" margin={{ top: 4, right: 24, left: 8, bottom: 0 }}>
            <CartesianGrid horizontal={false} stroke={CHART.rule} />
            <XAxis
              type="number"
              tickFormatter={(v) => formatDurationSeconds(v)}
              tick={AXIS_STYLE}
              axisLine={false}
              tickLine={false}
            />
            <YAxis type="category" dataKey="label" width={90} tick={AXIS_STYLE} axisLine={false} tickLine={false} />
            <Tooltip
              contentStyle={TOOLTIP_STYLE}
              formatter={(v, _n, item) => [
                `${formatDurationSeconds(v as number)} total · p50 ${formatDurationSeconds(item.payload.p50Seconds)}`,
                `${item.payload.issueCount} issues`,
              ]}
            />
            <Bar dataKey="totalSeconds" fill={CHART.signal} radius={[0, 2, 2, 0]} maxBarSize={22} />
          </BarChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
}
