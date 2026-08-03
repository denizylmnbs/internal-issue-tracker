import { apiData, toQuery } from "../client";
import type { ActivityResponse, PagedResponse } from "../types";

export const listProjectActivity = (
  projectId: number,
  query: { page?: number; size?: number; sort?: string } = {},
) =>
  apiData<PagedResponse<ActivityResponse>>(
    `/api/projects/${projectId}/activities${toQuery({ sort: "createdAt,desc", size: 50, ...query })}`,
  );

export const listIssueActivity = (
  projectId: number,
  issueId: number,
  query: { page?: number; size?: number; sort?: string } = {},
) =>
  apiData<PagedResponse<ActivityResponse>>(
    `/api/projects/${projectId}/issues/${issueId}/activities${toQuery({ sort: "createdAt,asc", size: 200, ...query })}`,
  );

export const listSprintActivity = (
  projectId: number,
  sprintId: number,
  query: { page?: number; size?: number; sort?: string } = {},
) =>
  apiData<PagedResponse<ActivityResponse>>(
    `/api/projects/${projectId}/sprints/${sprintId}/activities${toQuery({ sort: "createdAt,desc", size: 50, ...query })}`,
  );
