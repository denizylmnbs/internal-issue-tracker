"use client";

import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";
import { ChartCard } from "./ChartCard";
import { CHART, TOOLTIP_STYLE, AXIS_STYLE } from "./colors";
import { densify } from "@/lib/metrics/densify";
import { formatDateOnly, formatPercent, formatRatio } from "@/lib/format";
import type { DefectRatioResponse } from "@/lib/api/types";
import type { MetricsBucket } from "@/lib/api/enums";

/** Both denominators are shown because neither alone is enough (docs/API.md
 * §4.13) — the line tracks bug share of intake; the tooltip carries the
 * density-per-point figure alongside it rather than as a second axis. */
export function DefectRatioChart({
  data,
  bucket,
  isLoading,
}: {
  data: DefectRatioResponse | undefined;
  bucket: MetricsBucket;
  isLoading: boolean;
}) {
  const points = data
    ? densify(data.points, data.window.from, data.window.to, bucket, (bucketStart) => ({
        bucketStart,
        createdCount: 0,
        createdBugCount: 0,
        createdBugShare: 0,
        completedCount: 0,
        completedBugCount: 0,
        completedStoryPoints: 0,
        defectsPerCompletedIssue: null,
        defectsPerCompletedPoint: null,
      }))
    : [];

  return (
    <ChartCard title="Defect ratio" subtitle="bug share of intake">
      {isLoading ? (
        <div className="h-48 animate-pulse rounded bg-secondary" />
      ) : (
        <ResponsiveContainer width="100%" height={200}>
          <LineChart data={points} margin={{ top: 4, right: 4, left: -12, bottom: 0 }}>
            <CartesianGrid vertical={false} stroke={CHART.rule} />
            <XAxis
              dataKey="bucketStart"
              tickFormatter={formatDateOnly}
              tick={AXIS_STYLE}
              axisLine={{ stroke: CHART.rule }}
              tickLine={false}
            />
            <YAxis
              tickFormatter={(v) => formatPercent(v)}
              tick={AXIS_STYLE}
              axisLine={false}
              tickLine={false}
              domain={[0, "dataMax"]}
            />
            <Tooltip
              contentStyle={TOOLTIP_STYLE}
              labelFormatter={(v) => formatDateOnly(v as string)}
              formatter={(_v, _n, item) => [
                `${formatPercent(item.payload.createdBugShare)} of intake · ${formatRatio(item.payload.defectsPerCompletedPoint)} defects/pt`,
                "Bug share",
              ]}
            />
            <Line type="monotone" dataKey="createdBugShare" stroke={CHART.rust} strokeWidth={2} dot={{ r: 3 }} />
          </LineChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
}
