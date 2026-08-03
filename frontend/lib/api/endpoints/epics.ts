import { apiData, apiVoid, json, toQuery } from "../client";
import type {
  ChangeEpicStatusRequest,
  CreateEpicRequest,
  EpicResponse,
  PagedResponse,
  UpdateEpicRequest,
} from "../types";
import type { EpicStatus } from "../enums";

export const createEpic = (projectId: number, body: CreateEpicRequest) =>
  apiData<EpicResponse>(`/api/projects/${projectId}/epics`, json(body, "POST"));

export const getEpic = (projectId: number, epicId: number) =>
  apiData<EpicResponse>(`/api/projects/${projectId}/epics/${epicId}`);

export type ListEpicsQuery = {
  name?: string;
  status?: EpicStatus;
  reporterId?: number;
  page?: number;
  size?: number;
  sort?: string;
};

export const listEpics = (projectId: number, query: ListEpicsQuery = {}) =>
  apiData<PagedResponse<EpicResponse>>(
    `/api/projects/${projectId}/epics${toQuery({ sort: "createdAt,desc", size: 100, ...query })}`,
  );

export const updateEpic = (projectId: number, epicId: number, body: UpdateEpicRequest) =>
  apiData<EpicResponse>(`/api/projects/${projectId}/epics/${epicId}`, json(body, "PUT"));

export const changeEpicStatus = (
  projectId: number,
  epicId: number,
  body: ChangeEpicStatusRequest,
) =>
  apiData<EpicResponse>(
    `/api/projects/${projectId}/epics/${epicId}/status`,
    json(body, "PATCH"),
  );

export const deleteEpic = (projectId: number, epicId: number) =>
  apiVoid(`/api/projects/${projectId}/epics/${epicId}`, { method: "DELETE" });
