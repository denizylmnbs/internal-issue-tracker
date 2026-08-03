"use client";

import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from "recharts";
import { ChartCard } from "./ChartCard";
import { CATEGORICAL, TOOLTIP_STYLE, AXIS_STYLE, CHART } from "./colors";
import { bucketStarts, bucketKey } from "@/lib/metrics/densify";
import { formatDateOnly } from "@/lib/format";
import type { ThroughputBreakdownResponse } from "@/lib/api/types";
import type { MetricsBucket } from "@/lib/api/enums";

/** A sparse matrix, not a grid (docs/API.md §4.13) — pivot into one row per
 * bucket with one column per dimension value, zero-filled, before charting. */
export function ThroughputBreakdownChart({
  data,
  bucket,
  isLoading,
}: {
  data: ThroughputBreakdownResponse | undefined;
  bucket: MetricsBucket;
  isLoading: boolean;
}) {
  const values = data ? Array.from(new Set(data.points.map((p) => p.value))).sort() : [];
  const rows = data
    ? bucketStarts(data.window.from, data.window.to, bucket).map((bucketStart) => {
        const row: Record<string, string | number> = { bucketStart };
        for (const v of values) row[v] = 0;
        for (const p of data.points.filter((p) => bucketKey(p.bucketStart) === bucketKey(bucketStart))) {
          row[p.value] = p.completedCount;
        }
        return row;
      })
    : [];

  return (
    <ChartCard title={`Throughput by ${data?.dimension.toLowerCase() ?? "type"}`}>
      {isLoading ? (
        <div className="h-48 animate-pulse rounded bg-secondary" />
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={rows} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
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
            {values.length > 1 && <Legend wrapperStyle={{ fontSize: 12 }} />}
            {values.map((v, i) => (
              <Bar
                key={v}
                dataKey={v}
                stackId="a"
                fill={CATEGORICAL[i % CATEGORICAL.length]}
                radius={i === values.length - 1 ? [2, 2, 0, 0] : undefined}
                maxBarSize={28}
              />
            ))}
          </BarChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
}
