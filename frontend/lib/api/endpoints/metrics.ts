import { apiData, toQuery } from "../client";
import type {
  BurndownResponse,
  CumulativeFlowResponse,
  DefectRatioResponse,
  DurationStatsResponse,
  FlowEfficiencyResponse,
  NetFlowResponse,
  ReopenRateResponse,
  ThroughputBreakdownResponse,
  ThroughputResponse,
  TimeInStatusResponse,
  VelocityResponse,
  WipResponse,
} from "../types";
import type { MetricsBucket, MetricsDimension } from "../enums";

export type MetricsWindowQuery = { from?: string; to?: string };

const base = (projectId: number) => `/api/projects/${projectId}/metrics`;

export const getCycleTime = (projectId: number, query: MetricsWindowQuery = {}) =>
  apiData<DurationStatsResponse>(`${base(projectId)}/cycle-time${toQuery(query)}`);

export const getLeadTime = (projectId: number, query: MetricsWindowQuery = {}) =>
  apiData<DurationStatsResponse>(`${base(projectId)}/lead-time${toQuery(query)}`);

export const getBugMttr = (projectId: number, query: MetricsWindowQuery = {}) =>
  apiData<DurationStatsResponse>(`${base(projectId)}/bug-mttr${toQuery(query)}`);

export const getThroughput = (
  projectId: number,
  query: MetricsWindowQuery & { bucket?: MetricsBucket } = {},
) => apiData<ThroughputResponse>(`${base(projectId)}/throughput${toQuery(query)}`);

export const getThroughputBreakdown = (
  projectId: number,
  query: MetricsWindowQuery & { bucket?: MetricsBucket; dimension?: MetricsDimension } = {},
) =>
  apiData<ThroughputBreakdownResponse>(
    `${base(projectId)}/throughput-breakdown${toQuery(query)}`,
  );

export const getTimeInStatus = (projectId: number, query: MetricsWindowQuery = {}) =>
  apiData<TimeInStatusResponse>(`${base(projectId)}/time-in-status${toQuery(query)}`);

export const getFlowEfficiency = (projectId: number, query: MetricsWindowQuery = {}) =>
  apiData<FlowEfficiencyResponse>(`${base(projectId)}/flow-efficiency${toQuery(query)}`);

export const getReopenRate = (projectId: number, query: MetricsWindowQuery = {}) =>
  apiData<ReopenRateResponse>(`${base(projectId)}/reopen-rate${toQuery(query)}`);

export const getNetFlow = (
  projectId: number,
  query: MetricsWindowQuery & { bucket?: MetricsBucket } = {},
) => apiData<NetFlowResponse>(`${base(projectId)}/net-flow${toQuery(query)}`);

export const getDefectRatio = (
  projectId: number,
  query: MetricsWindowQuery & { bucket?: MetricsBucket } = {},
) => apiData<DefectRatioResponse>(`${base(projectId)}/defect-ratio${toQuery(query)}`);

export const getWip = (projectId: number, query: { asOf?: string } = {}) =>
  apiData<WipResponse>(`${base(projectId)}/wip${toQuery(query)}`);

export const getVelocity = (projectId: number) =>
  apiData<VelocityResponse>(`${base(projectId)}/velocity`);

export const getBurndown = (projectId: number, sprintId: number) =>
  apiData<BurndownResponse>(`${base(projectId)}/burndown${toQuery({ sprintId })}`);

export const getCfd = (projectId: number, query: MetricsWindowQuery = {}) =>
  apiData<CumulativeFlowResponse>(`${base(projectId)}/cfd${toQuery(query)}`);
