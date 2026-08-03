import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as projects from "@/lib/api/endpoints/projects";
import type {
  CreateProjectRequest,
  UpdateProjectRequest,
  ChangeProjectStatusRequest,
  ChangeProjectLeaderRequest,
} from "@/lib/api/types";
import type { ListProjectsQuery } from "@/lib/api/endpoints/projects";

export const projectKeys = {
  all: ["projects"] as const,
  list: (query: ListProjectsQuery) => ["projects", "list", query] as const,
  detail: (id: number) => ["projects", "detail", id] as const,
  participants: (id: number) => ["projects", id, "participants"] as const,
  members: (id: number) => ["projects", id, "members"] as const,
  teams: (id: number) => ["projects", id, "teams"] as const,
};

export const useProjectsList = (query: ListProjectsQuery) =>
  useQuery({ queryKey: projectKeys.list(query), queryFn: () => projects.listProjects(query) });

export const useProject = (id: number) =>
  useQuery({ queryKey: projectKeys.detail(id), queryFn: () => projects.getProject(id) });

/** `/participants` — everyone who works on the project, direct or via team.
 * This is the set permission checks and pickers use; never `/members`. */
export const useProjectParticipants = (id: number) =>
  useQuery({
    queryKey: projectKeys.participants(id),
    queryFn: () => projects.listProjectParticipants(id, { size: 200 }),
  });

/** `/members` — direct assignment rows only. Settings CRUD only. */
export const useProjectMembers = (id: number) =>
  useQuery({
    queryKey: projectKeys.members(id),
    queryFn: () => projects.listProjectMembers(id, { size: 200 }),
  });

export const useProjectTeams = (id: number) =>
  useQuery({
    queryKey: projectKeys.teams(id),
    queryFn: () => projects.listProjectTeams(id, { size: 200 }),
  });

export function useCreateProject() {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: CreateProjectRequest) => projects.createProject(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.all }),
  });
}

export function useUpdateProject(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: UpdateProjectRequest) => projects.updateProject(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.detail(id) }),
  });
}

export function useChangeProjectStatus(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: ChangeProjectStatusRequest) => projects.changeProjectStatus(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.detail(id) }),
  });
}

export function useChangeProjectLeader(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: ChangeProjectLeaderRequest) => projects.changeProjectLeader(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.detail(id) }),
  });
}

export function useRemoveProjectLeader(id: number) {
  const queryClient = useQueryClient();
  return useApiMutation<Awaited<ReturnType<typeof projects.removeProjectLeader>>, void>({
    mutationFn: () => projects.removeProjectLeader(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.detail(id) }),
  });
}

export function useDeleteProject() {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (id: number) => projects.deleteProject(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.all }),
  });
}
