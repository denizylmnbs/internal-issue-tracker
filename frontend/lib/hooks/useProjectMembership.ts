import { useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as projects from "@/lib/api/endpoints/projects";
import { projectKeys } from "./useProjects";

export function useAddProjectMember(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (userId: number) => projects.addProjectMember(projectId, { userId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.members(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.participants(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) });
    },
  });
}

export function useRemoveProjectMember(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (userId: number) => projects.removeProjectMember(projectId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.members(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.participants(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) });
    },
  });
}

export function useAddProjectTeam(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (teamId: number) => projects.addProjectTeam(projectId, { teamId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.teams(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.participants(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) });
    },
  });
}

export function useRemoveProjectTeam(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (teamId: number) => projects.removeProjectTeam(projectId, teamId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.teams(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.participants(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) });
    },
  });
}
