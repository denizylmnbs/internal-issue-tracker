import { apiData, apiVoid, json, toQuery } from "../client";
import type {
  ChangeSprintStatusRequest,
  CreateSprintRequest,
  PagedResponse,
  SprintResponse,
  UpdateSprintRequest,
} from "../types";

export const createSprint = (projectId: number, body: CreateSprintRequest) =>
  apiData<SprintResponse>(`/api/projects/${projectId}/sprints`, json(body, "POST"));

export const getSprint = (projectId: number, sprintId: number) =>
  apiData<SprintResponse>(`/api/projects/${projectId}/sprints/${sprintId}`);

export type ListSprintsQuery = {
  name?: string;
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export const listSprints = (projectId: number, query: ListSprintsQuery = {}) =>
  apiData<PagedResponse<SprintResponse>>(
    `/api/projects/${projectId}/sprints${toQuery({ sort: "startDate,desc", size: 100, ...query })}`,
  );

export const updateSprint = (
  projectId: number,
  sprintId: number,
  body: UpdateSprintRequest,
) =>
  apiData<SprintResponse>(
    `/api/projects/${projectId}/sprints/${sprintId}`,
    json(body, "PUT"),
  );

export const changeSprintStatus = (
  projectId: number,
  sprintId: number,
  body: ChangeSprintStatusRequest,
) =>
  apiData<SprintResponse>(
    `/api/projects/${projectId}/sprints/${sprintId}/status`,
    json(body, "PATCH"),
  );

export const deleteSprint = (projectId: number, sprintId: number) =>
  apiVoid(`/api/projects/${projectId}/sprints/${sprintId}`, { method: "DELETE" });
