"use client";

import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";
import { ChartCard } from "./ChartCard";
import { CHART, TOOLTIP_STYLE, AXIS_STYLE } from "./colors";
import { densify } from "@/lib/metrics/densify";
import { formatDateOnly } from "@/lib/format";
import type { ThroughputResponse } from "@/lib/api/types";
import type { MetricsBucket } from "@/lib/api/enums";

export function ThroughputChart({
  data,
  bucket,
  isLoading,
}: {
  data: ThroughputResponse | undefined;
  bucket: MetricsBucket;
  isLoading: boolean;
}) {
  const points = data
    ? densify(data.points, data.window.from, data.window.to, bucket, (bucketStart) => ({
        bucketStart,
        completedCount: 0,
      }))
    : [];

  return (
    <ChartCard title="Throughput" subtitle={data ? `completed per ${bucket.toLowerCase()}` : undefined}>
      {isLoading ? (
        <div className="h-48 animate-pulse rounded bg-secondary" />
      ) : (
        <ResponsiveContainer width="100%" height={200}>
          <BarChart data={points} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
            <CartesianGrid vertical={false} stroke={CHART.rule} />
            <XAxis
              dataKey="bucketStart"
              tickFormatter={formatDateOnly}
              tick={AXIS_STYLE}
              axisLine={{ stroke: CHART.rule }}
              tickLine={false}
            />
            <YAxis allowDecimals={false} tick={AXIS_STYLE} axisLine={false} tickLine={false} />
            <Tooltip
              contentStyle={TOOLTIP_STYLE}
              labelFormatter={(v) => formatDateOnly(v as string)}
              formatter={(v) => [v, "Completed"]}
            />
            <Bar dataKey="completedCount" fill={CHART.signal} radius={[2, 2, 0, 0]} maxBarSize={28} />
          </BarChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
}
