import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as teams from "@/lib/api/endpoints/teams";
import type {
  CreateTeamRequest,
  UpdateTeamRequest,
  ChangeTeamLeaderRequest,
  AddTeamMemberRequest,
} from "@/lib/api/types";
import type { ListTeamsQuery } from "@/lib/api/endpoints/teams";

const teamKeys = {
  all: ["teams"] as const,
  list: (query: ListTeamsQuery) => ["teams", "list", query] as const,
  detail: (id: number) => ["teams", "detail", id] as const,
  members: (id: number) => ["teams", id, "members"] as const,
};

export const useTeamsList = (query: ListTeamsQuery = {}) =>
  useQuery({ queryKey: teamKeys.list(query), queryFn: () => teams.listTeams(query) });

export const useTeam = (id: number) =>
  useQuery({ queryKey: teamKeys.detail(id), queryFn: () => teams.getTeam(id), enabled: !!id });

export const useTeamMembers = (teamId: number) =>
  useQuery({
    queryKey: teamKeys.members(teamId),
    queryFn: () => teams.listTeamMembers(teamId, { size: 200 }),
    enabled: !!teamId,
  });

export function useCreateTeam() {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: CreateTeamRequest) => teams.createTeam(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: teamKeys.all }),
  });
}

export function useUpdateTeam(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: UpdateTeamRequest) => teams.updateTeam(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: teamKeys.detail(id) }),
  });
}

export function useChangeTeamLeader(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: ChangeTeamLeaderRequest) => teams.changeTeamLeader(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: teamKeys.detail(id) }),
  });
}

export function useDeleteTeam() {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (id: number) => teams.deleteTeam(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: teamKeys.all }),
  });
}

export function useAddTeamMember(teamId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: AddTeamMemberRequest) => teams.addTeamMember(teamId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: teamKeys.members(teamId) }),
  });
}

export function useRemoveTeamMember(teamId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (userId: number) => teams.removeTeamMember(teamId, userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: teamKeys.members(teamId) }),
  });
}
