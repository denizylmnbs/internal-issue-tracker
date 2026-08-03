"use client";

import { ComposedChart, Bar, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from "recharts";
import { ChartCard } from "./ChartCard";
import { CHART, TOOLTIP_STYLE, AXIS_STYLE } from "./colors";
import { densify } from "@/lib/metrics/densify";
import { formatDateOnly } from "@/lib/format";
import type { NetFlowResponse } from "@/lib/api/types";
import type { MetricsBucket } from "@/lib/api/enums";

/** Bars (created/completed) and the cumulative-net line share one axis —
 * they're the same unit, issue counts, so this isn't the dual-axis mistake. */
export function NetFlowChart({
  data,
  bucket,
  isLoading,
}: {
  data: NetFlowResponse | undefined;
  bucket: MetricsBucket;
  isLoading: boolean;
}) {
  const points = data
    ? densify(data.points, data.window.from, data.window.to, bucket, (bucketStart) => ({
        bucketStart,
        createdCount: 0,
        completedCount: 0,
        netCount: 0,
        cumulativeNetCount: 0,
      }))
    : [];

  return (
    <ChartCard title="Net flow" subtitle="cumulative backlog trend">
      {isLoading ? (
        <div className="h-48 animate-pulse rounded bg-secondary" />
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <ComposedChart data={points} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
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
            <Bar dataKey="createdCount" name="Created" fill={CHART.slate} radius={[2, 2, 0, 0]} maxBarSize={20} />
            <Bar dataKey="completedCount" name="Completed" fill={CHART.moss} radius={[2, 2, 0, 0]} maxBarSize={20} />
            <Line
              type="monotone"
              dataKey="cumulativeNetCount"
              name="Cumulative net"
              stroke={CHART.signal}
              strokeWidth={2}
              dot={false}
            />
          </ComposedChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
}
