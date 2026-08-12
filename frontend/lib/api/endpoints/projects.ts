import { apiData, apiVoid, json, toQuery } from "../client";
import type {
  AddProjectMemberRequest,
  AddProjectTeamRequest,
  ChangeProjectLeaderRequest,
  ChangeProjectStatusRequest,
  CreateProjectRequest,
  PagedResponse,
  ProjectDetailResponse,
  ProjectMemberResponse,
  ProjectParticipantResponse,
  ProjectResponse,
  ProjectTeamResponse,
  UpdateProjectRequest,
} from "../types";

export const createProject = (body: CreateProjectRequest) =>
  apiData<ProjectResponse>("/api/projects", json(body, "POST"));

export const getProject = (id: number) =>
  apiData<ProjectDetailResponse>(`/api/projects/${id}`);

export type ListProjectsQuery = {
  name?: string;
  status?: string;
  leaderId?: number;
  startDateAfter?: string;
  endDateBefore?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export const listProjects = (query: ListProjectsQuery = {}) =>
  apiData<PagedResponse<ProjectResponse>>(
    `/api/projects${toQuery({ sort: "createdAt,desc", ...query })}`,
  );

export const updateProject = (id: number, body: UpdateProjectRequest) =>
  apiData<ProjectResponse>(`/api/projects/${id}`, json(body, "PUT"));

export const changeProjectLeader = (id: number, body: ChangeProjectLeaderRequest) =>
  apiData<ProjectResponse>(`/api/projects/${id}/leader`, json(body, "PATCH"));

export const removeProjectLeader = (id: number) =>
  apiData<ProjectResponse>(`/api/projects/${id}/leader`, { method: "DELETE" });

export const changeProjectStatus = (id: number, body: ChangeProjectStatusRequest) =>
  apiData<ProjectResponse>(`/api/projects/${id}/status`, json(body, "PATCH"));

export const deleteProject = (id: number) =>
  apiVoid(`/api/projects/${id}`, { method: "DELETE" });

// ---- members: direct assignment rows only — the set POST/DELETE act on ----

export const addProjectMember = (projectId: number, body: AddProjectMemberRequest) =>
  apiData<ProjectMemberResponse>(`/api/projects/${projectId}/members`, json(body, "POST"));

export const listProjectMembers = (
  projectId: number,
  query: { page?: number; size?: number } = {},
) =>
  apiData<PagedResponse<ProjectMemberResponse>>(
    // `joinedAt` is a DTO field, not an entity property on ProjectMember —
    // sorting by it 400s (INVALID_SORT_PROPERTY). `updatedAt` is the real
    // column it's derived from.
    `/api/projects/${projectId}/members${toQuery({ sort: "updatedAt,desc", ...query })}`,
  );

export const removeProjectMember = (projectId: number, userId: number) =>
  apiVoid(`/api/projects/${projectId}/members/${userId}`, { method: "DELETE" });

// ---- participants: everyone who works on the project, incl. via teams ----

export const listProjectParticipants = (
  projectId: number,
  query: { page?: number; size?: number } = {},
) =>
  apiData<PagedResponse<ProjectParticipantResponse>>(
    `/api/projects/${projectId}/participants${toQuery({ size: 200, ...query })}`,
  );

// ---------------------------------------------------------- project teams

export const addProjectTeam = (projectId: number, body: AddProjectTeamRequest) =>
  apiData<ProjectTeamResponse>(`/api/projects/${projectId}/teams`, json(body, "POST"));

export const listProjectTeams = (
  projectId: number,
  query: { page?: number; size?: number } = {},
) =>
  apiData<PagedResponse<ProjectTeamResponse>>(
    // `assignedAt` is a DTO field, not an entity property on ProjectTeam —
    // sorting by it 400s (INVALID_SORT_PROPERTY). `updatedAt` is the real
    // column it's derived from.
    `/api/projects/${projectId}/teams${toQuery({ sort: "updatedAt,desc", ...query })}`,
  );

export const removeProjectTeam = (projectId: number, teamId: number) =>
  apiVoid(`/api/projects/${projectId}/teams/${teamId}`, { method: "DELETE" });
