"use client";

import { AreaChart, Area, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from "recharts";
import { ChartCard } from "./ChartCard";
import { CATEGORICAL, TOOLTIP_STYLE, AXIS_STYLE, CHART } from "./colors";
import { bucketStarts, bucketKey } from "@/lib/metrics/densify";
import { formatDateOnly } from "@/lib/format";
import { useProjectContext } from "@/lib/project/ProjectContext";
import type { CumulativeFlowResponse } from "@/lib/api/types";

/** One row per day per occupied status — fixed daily bucketing, no control
 * (docs/API.md §4.13: a weekly cut would smooth away the queue it's drawn to show). */
export function CfdChart({ data, isLoading }: { data: CumulativeFlowResponse | undefined; isLoading: boolean }) {
  const { resolveField } = useProjectContext();
  const statuses = data ? Array.from(new Set(data.points.map((p) => p.status))) : [];
  const rows = data
    ? bucketStarts(data.window.from, data.window.to, "DAY").map((bucketStart) => {
        const row: Record<string, string | number> = { bucketStart };
        for (const s of statuses) row[s] = 0;
        for (const p of data.points.filter((p) => bucketKey(p.bucketStart) === bucketKey(bucketStart))) {
          row[p.status] = p.issueCount;
        }
        return row;
      })
    : [];

  return (
    <ChartCard title="Cumulative flow">
      {isLoading ? (
        <div className="h-56 animate-pulse rounded bg-secondary" />
      ) : rows.length === 0 || statuses.length === 0 ? (
        <p className="text-sm text-slate">No occupied statuses in this window.</p>
      ) : (
        <ResponsiveContainer width="100%" height={260}>
          <AreaChart data={rows} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
            <CartesianGrid vertical={false} stroke={CHART.rule} />
            <XAxis
              dataKey="bucketStart"
              tickFormatter={formatDateOnly}
              tick={AXIS_STYLE}
              axisLine={{ stroke: CHART.rule }}
              tickLine={false}
            />
            <YAxis allowDecimals={false} tick={AXIS_STYLE} axisLine={false} tickLine={false} />
            <Tooltip contentStyle={TOOLTIP_STYLE} labelFormatter={(v) => formatDateOnly(v as string)} />
            <Legend
              wrapperStyle={{ fontSize: 12 }}
              formatter={(value) => resolveField("ISSUE_STATUS", value as string)?.label ?? value}
            />
            {statuses.map((s, i) => (
              <Area
                key={s}
                type="monotone"
                dataKey={s}
                name={s}
                stackId="a"
                stroke={CATEGORICAL[i % CATEGORICAL.length]}
                fill={CATEGORICAL[i % CATEGORICAL.length]}
                fillOpacity={0.5}
              />
            ))}
          </AreaChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
}
