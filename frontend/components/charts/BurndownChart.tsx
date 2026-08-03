"use client";

import { LineChart, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from "recharts";
import { ChartCard } from "./ChartCard";
import { CHART, TOOLTIP_STYLE, AXIS_STYLE } from "./colors";
import { formatDateOnly } from "@/lib/format";
import type { BurndownResponse } from "@/lib/api/types";

/**
 * The ideal line is drawn client-side, from `committedPoints` at
 * `startDate` down to zero at `endDate` (docs/API.md §4.13) — the server
 * doesn't send it. `scopePoints` against `committedPoints` is where scope
 * creep shows, so it rides along as a second series rather than a note.
 */
export function BurndownChart({
  data,
  isLoading,
}: {
  data: BurndownResponse | undefined;
  isLoading: boolean;
}) {
  if (isLoading || !data) {
    return (
      <ChartCard title="Burndown">
        <div className="h-56 animate-pulse rounded bg-secondary" />
      </ChartCard>
    );
  }

  const start = new Date(data.startDate).getTime();
  const end = new Date(data.endDate).getTime();
  const totalMs = Math.max(end - start, 1);

  const rows = data.points.map((p) => {
    const t = new Date(p.bucketStart).getTime();
    const fraction = Math.min(1, Math.max(0, (t - start) / totalMs));
    const ideal = data.committedPoints != null ? data.committedPoints * (1 - fraction) : null;
    return { ...p, ideal: ideal != null ? Math.round(ideal * 10) / 10 : null };
  });

  return (
    <ChartCard
      title={`Burndown — ${data.name}`}
      subtitle={`${formatDateOnly(data.startDate)} – ${formatDateOnly(data.endDate)}`}
    >
      {rows.length === 0 ? (
        <p className="text-sm text-slate">No points yet — the sprint hasn't started.</p>
      ) : (
        <ResponsiveContainer width="100%" height={240}>
          <LineChart data={rows} margin={{ top: 4, right: 4, left: -12, bottom: 0 }}>
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
            <Legend wrapperStyle={{ fontSize: 12 }} />
            {data.committedPoints != null && (
              <Line
                type="linear"
                dataKey="ideal"
                name="Ideal"
                stroke={CHART.slate}
                strokeDasharray="4 3"
                strokeWidth={1.5}
                dot={false}
              />
            )}
            <Line
              type="monotone"
              dataKey="remainingPoints"
              name="Remaining"
              stroke={CHART.signal}
              strokeWidth={2}
              dot={{ r: 3 }}
            />
            <Line
              type="monotone"
              dataKey="scopePoints"
              name="Scope"
              stroke={CHART.amber}
              strokeWidth={1.5}
              dot={false}
            />
          </LineChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
}
