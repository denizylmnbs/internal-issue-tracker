"use client";

import { useState } from "react";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useSprintsList } from "@/lib/hooks/useSprints";
import {
  useCycleTime,
  useLeadTime,
  useBugMttr,
  useThroughput,
  useThroughputBreakdown,
  useTimeInStatus,
  useFlowEfficiency,
  useReopenRate,
  useNetFlow,
  useDefectRatio,
  useWip,
  useVelocity,
  useBurndown,
  useCfd,
} from "@/lib/hooks/useMetrics";
import { defaultWindow } from "@/lib/metrics/densify";
import { DurationCard } from "@/components/charts/DurationCard";
import { ThroughputChart } from "@/components/charts/ThroughputChart";
import { ThroughputBreakdownChart } from "@/components/charts/ThroughputBreakdownChart";
import { NetFlowChart } from "@/components/charts/NetFlowChart";
import { CfdChart } from "@/components/charts/CfdChart";
import { TimeInStatusChart } from "@/components/charts/TimeInStatusChart";
import { FlowEfficiencyCard, ReopenRateCard } from "@/components/charts/SimpleStatCards";
import { DefectRatioChart } from "@/components/charts/DefectRatioChart";
import { WipPanel } from "@/components/charts/WipPanel";
import { VelocityChart } from "@/components/charts/VelocityChart";
import { BurndownChart } from "@/components/charts/BurndownChart";
import { SprintForecastBanner, isForecastable } from "@/components/sprint/SprintForecast";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { METRICS_BUCKETS, METRICS_DIMENSIONS } from "@/lib/api/enums";
import type { MetricsBucket, MetricsDimension } from "@/lib/api/enums";

function toDateInput(iso: string) {
  return iso.slice(0, 10);
}

/** `<input type="date">` reports `value === ""` while the date is incomplete
 * (e.g. a year field that only has "0" typed into it so far) — `new
 * Date("").toISOString()` throws a `RangeError` synchronously in the change
 * handler, which is what was crashing the page. Returns null for "still
 * typing" as well as for a value that parses but isn't a real date, so the
 * caller can just ignore the event and leave the window as it was. */
function parseDateInput(value: string): string | null {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toISOString();
}

/** A floor for the date inputs — otherwise a technically-valid but absurd
 * date (year 1, typed digit by digit before the rest of the field is filled
 * in) produces a window `densify.ts#bucketStarts` walks one bucket at a time
 * from, all the way to today: hundreds of thousands of iterations across
 * five charts, which reads as the tab hanging rather than erroring. */
const EARLIEST_WINDOW_DATE = "2000-01-01";

export default function InsightsPage() {
  const { projectId, project } = useProjectContext();
  const [window, setWindow] = useState(defaultWindow());
  const [bucket, setBucket] = useState<MetricsBucket>("WEEK");
  const [dimension, setDimension] = useState<MetricsDimension>("TYPE");
  const [sprintId, setSprintId] = useState<number | undefined>();

  const { data: sprints } = useSprintsList(projectId, { size: 100, sort: "startDate,desc" });

  const cycleTime = useCycleTime(projectId, window);
  const leadTime = useLeadTime(projectId, window);
  const bugMttr = useBugMttr(projectId, window);
  const throughput = useThroughput(projectId, window, bucket);
  const throughputBreakdown = useThroughputBreakdown(projectId, window, bucket, dimension);
  const netFlow = useNetFlow(projectId, window, bucket);
  const cfd = useCfd(projectId, window);
  const timeInStatus = useTimeInStatus(projectId, window);
  const flowEfficiency = useFlowEfficiency(projectId, window);
  const reopenRate = useReopenRate(projectId, window);
  const defectRatio = useDefectRatio(projectId, window, bucket);
  const wip = useWip(projectId);
  const velocity = useVelocity(projectId);
  const effectiveSprintId = sprintId ?? sprints?.content.find((s) => s.status === "IN_PROGRESS")?.id;
  const burndown = useBurndown(projectId, effectiveSprintId);

  // the forecast rides on the same sprint the burndown is drawn for
  const selectedSprint = sprints?.content.find((s) => s.id === effectiveSprintId);
  const forecastSprint =
    selectedSprint && isForecastable(selectedSprint) ? selectedSprint : undefined;

  return (
    <div className="max-w-5xl p-6">
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <h1 className="font-heading text-xl font-semibold tracking-tight">Insights</h1>
        <div className="flex flex-wrap items-end gap-2">
          <div className="space-y-1">
            <Label className="text-xs text-slate">From</Label>
            <Input
              type="date"
              value={toDateInput(window.from)}
              min={project?.startDate ? toDateInput(project.startDate) : EARLIEST_WINDOW_DATE}
              max={toDateInput(window.to)}
              onChange={(e) => {
                const from = parseDateInput(e.target.value);
                if (!from) return;
                // Keep from <= to rather than silently sending an inverted
                // window to the backend, which doesn't validate the order
                // and would echo it straight back.
                setWindow((w) => (from > w.to ? { from, to: from } : { ...w, from }));
              }}
              className="h-8 w-36"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs text-slate">To</Label>
            <Input
              type="date"
              value={toDateInput(window.to)}
              min={toDateInput(window.from)}
              onChange={(e) => {
                const to = parseDateInput(e.target.value);
                if (!to) return;
                setWindow((w) => (to < w.from ? { from: to, to } : { ...w, to }));
              }}
              className="h-8 w-36"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs text-slate">Bucket</Label>
            <Select value={bucket} onValueChange={(v) => setBucket(v as MetricsBucket)}>
              <SelectTrigger className="h-8 w-28">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {METRICS_BUCKETS.map((b) => (
                  <SelectItem key={b} value={b}>{b}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      <section className="mb-8">
        <h2 className="mb-3 font-heading text-xs font-semibold uppercase tracking-wide text-slate">
          Duration
        </h2>
        <div className="grid gap-3 sm:grid-cols-3">
          <DurationCard title="Cycle time" data={cycleTime.data} isLoading={cycleTime.isLoading} />
          <DurationCard title="Lead time" data={leadTime.data} isLoading={leadTime.isLoading} />
          <DurationCard title="Bug MTTR" data={bugMttr.data} isLoading={bugMttr.isLoading} />
        </div>
      </section>

      <section className="mb-8">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-heading text-xs font-semibold uppercase tracking-wide text-slate">Flow</h2>
          <Select value={dimension} onValueChange={(v) => setDimension(v as MetricsDimension)}>
            <SelectTrigger className="h-7 w-32 text-xs">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {METRICS_DIMENSIONS.map((d) => (
                <SelectItem key={d} value={d}>by {d.toLowerCase()}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <ThroughputChart data={throughput.data} bucket={bucket} isLoading={throughput.isLoading} />
          <ThroughputBreakdownChart
            data={throughputBreakdown.data}
            bucket={bucket}
            isLoading={throughputBreakdown.isLoading}
          />
          <NetFlowChart data={netFlow.data} bucket={bucket} isLoading={netFlow.isLoading} />
          <CfdChart data={cfd.data} isLoading={cfd.isLoading} />
        </div>
      </section>

      <section className="mb-8">
        <h2 className="mb-3 font-heading text-xs font-semibold uppercase tracking-wide text-slate">
          Quality
        </h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <TimeInStatusChart data={timeInStatus.data} isLoading={timeInStatus.isLoading} />
          <DefectRatioChart data={defectRatio.data} bucket={bucket} isLoading={defectRatio.isLoading} />
          <ReopenRateCard data={reopenRate.data} isLoading={reopenRate.isLoading} />
          <FlowEfficiencyCard data={flowEfficiency.data} isLoading={flowEfficiency.isLoading} />
        </div>
      </section>

      <section>
        <h2 className="mb-3 font-heading text-xs font-semibold uppercase tracking-wide text-slate">
          Delivery
        </h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <VelocityChart data={velocity.data} isLoading={velocity.isLoading} />
          <WipPanel projectId={projectId} data={wip.data} isLoading={wip.isLoading} />
          <div className="sm:col-span-2">
            <div className="mb-2 flex items-center justify-between">
              <span className="text-xs font-medium text-slate">Sprint</span>
              <Select
                value={effectiveSprintId ? String(effectiveSprintId) : undefined}
                onValueChange={(v) => setSprintId(Number(v))}
              >
                <SelectTrigger className="h-8 w-56">
                  <SelectValue placeholder="Select a sprint" />
                </SelectTrigger>
                <SelectContent>
                  {sprints?.content.map((s) => (
                    <SelectItem key={s.id} value={String(s.id)}>{s.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            {forecastSprint && (
              <div className="mb-2">
                <SprintForecastBanner projectId={projectId} sprint={forecastSprint} />
              </div>
            )}
            <BurndownChart data={burndown.data} isLoading={burndown.isLoading} />
          </div>
        </div>
      </section>
    </div>
  );
}
