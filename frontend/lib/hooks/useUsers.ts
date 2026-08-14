import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as users from "@/lib/api/endpoints/users";
import { SESSION_QUERY_KEY } from "@/lib/auth/session";
import type {
  UpdateUserRequest,
  ChangePasswordRequest,
  ResetPasswordRequest,
  ChangeRoleRequest,
} from "@/lib/api/types";
import type { ListUsersQuery } from "@/lib/api/endpoints/users";

const userKeys = {
  list: (query: ListUsersQuery) => ["users", "list", query] as const,
  detail: (id: number) => ["users", "detail", id] as const,
  teams: (id: number) => ["users", id, "teams"] as const,
  projects: (id: number) => ["users", id, "projects"] as const,
  directory: ["users", "directory"] as const,
};

export const useUsersList = (query: ListUsersQuery = {}) =>
  useQuery({ queryKey: userKeys.list(query), queryFn: () => users.listUsers(query) });

export const useUserDetail = (id: number) =>
  useQuery({ queryKey: userKeys.detail(id), queryFn: () => users.getUser(id), enabled: !!id });

export const useUserTeamsList = (id: number) =>
  useQuery({ queryKey: userKeys.teams(id), queryFn: () => users.getUserTeams(id, { size: 100 }), enabled: !!id });

export const useUserProjectsList = (id: number) =>
  useQuery({
    queryKey: userKeys.projects(id),
    queryFn: () => users.getUserProjects(id, { size: 100 }),
    enabled: !!id,
  });

function invalidateUserLists(queryClient: ReturnType<typeof useQueryClient>, id: number) {
  queryClient.invalidateQueries({ queryKey: ["users"] });
  queryClient.invalidateQueries({ queryKey: userKeys.detail(id) });
}

export function useChangeUserRole(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: ChangeRoleRequest) => users.changeRole(id, body),
    onSuccess: () => invalidateUserLists(queryClient, id),
  });
}

export function useDeactivateUser() {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (id: number) => users.deleteUser(id),
    onSuccess: (_data, id) => invalidateUserLists(queryClient, id),
  });
}

export function useResetPassword(id: number) {
  return useApiMutation({
    mutationFn: (body: ResetPasswordRequest) => users.resetPassword(id, body),
  });
}

export function useUpdateProfile(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: UpdateUserRequest) => users.updateUser(id, body),
    onSuccess: () => invalidateUserLists(queryClient, id),
  });
}

export function useChangeOwnPassword(id: number) {
  return useApiMutation({
    mutationFn: (body: ChangePasswordRequest) => users.changePassword(id, body),
  });
}

// Both avatar mutations invalidate the session query too, not just the user lists: AppRail reads
// the logged-in user's avatar from the session cache, not from useUserDetail, so skipping this
// leaves the sidebar showing the old picture (or initials) after a user changes their own.
function invalidateAvatar(queryClient: ReturnType<typeof useQueryClient>, id: number) {
  invalidateUserLists(queryClient, id);
  queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY });
}

export function useUploadAvatar(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (file: File) => users.uploadAvatar(id, file),
    onSuccess: () => invalidateAvatar(queryClient, id),
  });
}

export function useRemoveAvatar(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: () => users.deleteAvatar(id),
    onSuccess: () => invalidateAvatar(queryClient, id),
  });
}
