import { apiData, apiVoid, json, toQuery } from "../client";
import type {
  ChangePasswordRequest,
  ChangeRoleRequest,
  PagedResponse,
  RegisterRequest,
  ResetPasswordRequest,
  UpdateUserRequest,
  UserProjectMembershipResponse,
  UserResponse,
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
    `/api/users/${id}/teams${toQuery({ sort: "joinedAt,desc", ...query })}`,
  );

export const getUserProjects = (id: number, query: { page?: number; size?: number } = {}) =>
  apiData<PagedResponse<UserProjectMembershipResponse>>(
    `/api/users/${id}/projects${toQuery({ sort: "projectName,asc", ...query })}`,
  );
