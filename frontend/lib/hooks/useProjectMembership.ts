import { useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as projects from "@/lib/api/endpoints/projects";
import { projectKeys } from "./useProjects";

/** USER_ADDED/TEAM_ADDED etc. rows land in `project_activities` via an async
 * listener (`ProjectActivityListener`, `@ApplicationModuleListener` — after
 * commit, on another thread), so invalidating this alongside members/teams
 * doesn't guarantee the row exists the instant this resolves. It does mean
 * the Activity tab doesn't serve a `staleTime`-cached response from before
 * the change either, and navigating there refetches. */
const activityKey = (projectId: number) => ["projects", projectId, "activities"];

export function useAddProjectMember(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (userId: number) => projects.addProjectMember(projectId, { userId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.members(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.participants(projectId) });
      queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) });
      queryClient.invalidateQueries({ queryKey: activityKey(projectId) });
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
      queryClient.invalidateQueries({ queryKey: activityKey(projectId) });
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
      queryClient.invalidateQueries({ queryKey: activityKey(projectId) });
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
      queryClient.invalidateQueries({ queryKey: activityKey(projectId) });
    },
  });
}
