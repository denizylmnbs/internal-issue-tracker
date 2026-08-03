import { useQuery } from "@tanstack/react-query";
import * as metrics from "@/lib/api/endpoints/metrics";
import type { MetricsBucket, MetricsDimension } from "@/lib/api/enums";

export type Window = { from: string; to: string };

const keyOf = (name: string, projectId: number, params: object) =>
  ["metrics", projectId, name, params] as const;

export const useCycleTime = (projectId: number, w: Window) =>
  useQuery({ queryKey: keyOf("cycle-time", projectId, w), queryFn: () => metrics.getCycleTime(projectId, w) });

export const useLeadTime = (projectId: number, w: Window) =>
  useQuery({ queryKey: keyOf("lead-time", projectId, w), queryFn: () => metrics.getLeadTime(projectId, w) });

export const useBugMttr = (projectId: number, w: Window) =>
  useQuery({ queryKey: keyOf("bug-mttr", projectId, w), queryFn: () => metrics.getBugMttr(projectId, w) });

export const useThroughput = (projectId: number, w: Window, bucket: MetricsBucket) =>
  useQuery({
    queryKey: keyOf("throughput", projectId, { ...w, bucket }),
    queryFn: () => metrics.getThroughput(projectId, { ...w, bucket }),
  });

export const useThroughputBreakdown = (
  projectId: number,
  w: Window,
  bucket: MetricsBucket,
  dimension: MetricsDimension,
) =>
  useQuery({
    queryKey: keyOf("throughput-breakdown", projectId, { ...w, bucket, dimension }),
    queryFn: () => metrics.getThroughputBreakdown(projectId, { ...w, bucket, dimension }),
  });

export const useTimeInStatus = (projectId: number, w: Window) =>
  useQuery({ queryKey: keyOf("time-in-status", projectId, w), queryFn: () => metrics.getTimeInStatus(projectId, w) });

export const useFlowEfficiency = (projectId: number, w: Window) =>
  useQuery({ queryKey: keyOf("flow-efficiency", projectId, w), queryFn: () => metrics.getFlowEfficiency(projectId, w) });

export const useReopenRate = (projectId: number, w: Window) =>
  useQuery({ queryKey: keyOf("reopen-rate", projectId, w), queryFn: () => metrics.getReopenRate(projectId, w) });

export const useNetFlow = (projectId: number, w: Window, bucket: MetricsBucket) =>
  useQuery({
    queryKey: keyOf("net-flow", projectId, { ...w, bucket }),
    queryFn: () => metrics.getNetFlow(projectId, { ...w, bucket }),
  });

export const useDefectRatio = (projectId: number, w: Window, bucket: MetricsBucket) =>
  useQuery({
    queryKey: keyOf("defect-ratio", projectId, { ...w, bucket }),
    queryFn: () => metrics.getDefectRatio(projectId, { ...w, bucket }),
  });

export const useWip = (projectId: number) =>
  useQuery({ queryKey: keyOf("wip", projectId, {}), queryFn: () => metrics.getWip(projectId) });

export const useVelocity = (projectId: number) =>
  useQuery({ queryKey: keyOf("velocity", projectId, {}), queryFn: () => metrics.getVelocity(projectId) });

export const useBurndown = (projectId: number, sprintId: number | undefined) =>
  useQuery({
    queryKey: keyOf("burndown", projectId, { sprintId }),
    queryFn: () => metrics.getBurndown(projectId, sprintId as number),
    enabled: sprintId != null,
  });

export const useCfd = (projectId: number, w: Window) =>
  useQuery({ queryKey: keyOf("cfd", projectId, w), queryFn: () => metrics.getCfd(projectId, w) });
