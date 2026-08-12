import { apiData, apiVoid, json, toQuery } from "../client";
import type {
  AddTeamMemberRequest,
  ChangeTeamLeaderRequest,
  CreateTeamRequest,
  PagedResponse,
  TeamMemberResponse,
  TeamResponse,
  UpdateTeamRequest,
} from "../types";

export const createTeam = (body: CreateTeamRequest) =>
  apiData<TeamResponse>("/api/teams", json(body, "POST"));

export const getTeam = (id: number) => apiData<TeamResponse>(`/api/teams/${id}`);

export type ListTeamsQuery = {
  name?: string;
  field?: string;
  leaderId?: number;
  page?: number;
  size?: number;
  sort?: string;
};

export const listTeams = (query: ListTeamsQuery = {}) =>
  apiData<PagedResponse<TeamResponse>>(
    `/api/teams${toQuery({ sort: "name,asc", ...query })}`,
  );

export const updateTeam = (id: number, body: UpdateTeamRequest) =>
  apiData<TeamResponse>(`/api/teams/${id}`, json(body, "PUT"));

export const changeTeamLeader = (id: number, body: ChangeTeamLeaderRequest) =>
  apiData<TeamResponse>(`/api/teams/${id}/leader`, json(body, "PATCH"));

export const deleteTeam = (id: number) => apiVoid(`/api/teams/${id}`, { method: "DELETE" });

export const addTeamMember = (teamId: number, body: AddTeamMemberRequest) =>
  apiData<TeamMemberResponse>(`/api/teams/${teamId}/members`, json(body, "POST"));

export const listTeamMembers = (
  teamId: number,
  query: { page?: number; size?: number } = {},
) =>
  apiData<PagedResponse<TeamMemberResponse>>(
    `/api/teams/${teamId}/members${toQuery({ sort: "joinedAt,desc", ...query })}`,
  );

export const listAllTeamMemberships = (
  query: { page?: number; size?: number } = {},
) =>
  apiData<PagedResponse<TeamMemberResponse>>(
    `/api/teams/members${toQuery({ sort: "joinedAt,desc", ...query })}`,
  );

export const removeTeamMember = (teamId: number, userId: number) =>
  apiVoid(`/api/teams/${teamId}/members/${userId}`, { method: "DELETE" });
