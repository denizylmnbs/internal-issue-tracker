"use client";

import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from "recharts";
import { ChartCard } from "./ChartCard";
import { CHART, TOOLTIP_STYLE, AXIS_STYLE } from "./colors";
import { formatRatio } from "@/lib/format";
import type { VelocityResponse } from "@/lib/api/types";

/** Sprints not yet started are included with nulls, so the plan charts
 * alongside the history (docs/API.md §4.13). sayDoRatio is never coalesced
 * from a null committedPoints — that would make an unstarted-column sprint
 * look like a perfect hit. */
export function VelocityChart({ data, isLoading }: { data: VelocityResponse | undefined; isLoading: boolean }) {
  return (
    <ChartCard title="Velocity" subtitle="committed vs. delivered, per sprint">
      {isLoading || !data ? (
        <div className="h-56 animate-pulse rounded bg-secondary" />
      ) : data.sprints.length === 0 ? (
        <p className="text-sm text-slate">No sprints have run yet.</p>
      ) : (
        <ResponsiveContainer width="100%" height={240}>
          <BarChart data={data.sprints} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
            <CartesianGrid vertical={false} stroke={CHART.rule} />
            <XAxis dataKey="name" tick={AXIS_STYLE} axisLine={{ stroke: CHART.rule }} tickLine={false} />
            <YAxis allowDecimals={false} tick={AXIS_STYLE} axisLine={false} tickLine={false} />
            <Tooltip
              contentStyle={TOOLTIP_STYLE}
              formatter={(v, n, item) => {
                if (n === "committedPoints") return [v ?? "—", "Committed"];
                if (n === "completedPoints")
                  return [`${v ?? "—"} (say/do ${formatRatio(item.payload.sayDoRatio)})`, "Completed"];
                return [v, n];
              }}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} formatter={(v) => (v === "committedPoints" ? "Committed" : "Completed")} />
            <Bar dataKey="committedPoints" fill={CHART.slate} radius={[2, 2, 0, 0]} maxBarSize={22} />
            <Bar dataKey="completedPoints" fill={CHART.signal} radius={[2, 2, 0, 0]} maxBarSize={22} />
          </BarChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
}
