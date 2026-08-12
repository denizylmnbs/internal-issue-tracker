import { apiData, apiVoid, json, toQuery } from "../client";
import type {
  ChangePasswordRequest,
  ChangeRoleRequest,
  IssueResponse,
  PagedResponse,
  RegisterRequest,
  ResetPasswordRequest,
  UpdateUserRequest,
  UserProjectMembershipResponse,
  UserResponse,
  UserSprintProgressResponse,
  UserTeamMembershipResponse,
} from "../types";

export const register = (body: RegisterRequest) =>
  apiData<UserResponse>("/api/users/register", json(body, "POST"));

export const getMe = () => apiData<UserResponse>("/api/auth/me");

export const getUser = (id: number) => apiData<UserResponse>(`/api/users/${id}`);

export type ListUsersQuery = {
  name?: string;
  surname?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export const listUsers = (query: ListUsersQuery = {}) =>
  apiData<PagedResponse<UserResponse>>(
    `/api/users${toQuery({ sort: "name,asc", ...query })}`,
  );

export const updateUser = (id: number, body: UpdateUserRequest) =>
  apiData<UserResponse>(`/api/users/${id}`, json(body, "PUT"));

export const deleteUser = (id: number) => apiVoid(`/api/users/${id}`, { method: "DELETE" });

export const changePassword = (id: number, body: ChangePasswordRequest) =>
  apiData<UserResponse>(`/api/users/${id}/password`, json(body, "PATCH"));

export const resetPassword = (id: number, body: ResetPasswordRequest) =>
  apiVoid(`/api/users/${id}/reset-password`, json(body, "POST"));

export const changeRole = (id: number, body: ChangeRoleRequest) =>
  apiData<UserResponse>(`/api/users/${id}/role`, json(body, "PATCH"));

export const getUserTeams = (id: number, query: { page?: number; size?: number } = {}) =>
  apiData<PagedResponse<UserTeamMembershipResponse>>(
    // `joinedAt` is a DTO field, not an entity property — this endpoint sorts
    // against TeamMember directly and has no remap for it (unlike the
    // /api/teams/{id}/members routes), so `sort=joinedAt` 500s. `updatedAt`
    // is the real column it's derived from.
    `/api/users/${id}/teams${toQuery({ sort: "updatedAt,desc", ...query })}`,
  );

export const getUserProjects = (id: number, query: { page?: number; size?: number } = {}) =>
  apiData<PagedResponse<UserProjectMembershipResponse>>(
    `/api/users/${id}/projects${toQuery({ sort: "projectName,asc", ...query })}`,
  );

/** Active issues (TODO/IN_PROGRESS/IN_REVIEW) assigned to this user, across every project. */
export const getUserActiveIssues = (id: number, query: { page?: number; size?: number } = {}) =>
  apiData<PagedResponse<IssueResponse>>(
    `/api/users/${id}/issues${toQuery({ sort: "updatedAt,desc", ...query })}`,
  );

export const getUserSprintProgress = (id: number) =>
  apiData<UserSprintProgressResponse>(`/api/users/${id}/sprint-progress`);
