import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as sprints from "@/lib/api/endpoints/sprints";
import type {
  CreateSprintRequest,
  UpdateSprintRequest,
  ChangeSprintStatusRequest,
} from "@/lib/api/types";
import type { ListSprintsQuery } from "@/lib/api/endpoints/sprints";
import { isApiClientError } from "@/lib/api/errors";

const sprintKeys = {
  list: (projectId: number, query: ListSprintsQuery) =>
    ["projects", projectId, "sprints", "list", query] as const,
  detail: (projectId: number, sprintId: number) =>
    ["sprints", projectId, sprintId] as const,
  all: (projectId: number) => ["projects", projectId, "sprints"] as const,
};

export const useSprintsList = (projectId: number, query: ListSprintsQuery = {}) =>
  useQuery({
    queryKey: sprintKeys.list(projectId, query),
    queryFn: () => sprints.listSprints(projectId, query),
    enabled: !!projectId,
  });

/** docs/API.md §5 note 4: a sprintId on an issue can be dangling — degrade
 * to a "deleted" marker rather than error the page it appears on. */
export function useSprintSafe(projectId: number, sprintId: number | null | undefined) {
  const query = useQuery({
    queryKey: sprintKeys.detail(projectId, sprintId as number),
    queryFn: () => sprints.getSprint(projectId, sprintId as number),
    enabled: sprintId != null,
    retry: false,
  });
  const deleted = query.isError && isApiClientError(query.error) && query.error.status === 404;
  return { ...query, deleted };
}

export function useCreateSprint(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: CreateSprintRequest) => sprints.createSprint(projectId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: sprintKeys.all(projectId) }),
  });
}

export function useUpdateSprint(projectId: number, sprintId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: UpdateSprintRequest) => sprints.updateSprint(projectId, sprintId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: sprintKeys.all(projectId) }),
  });
}

export function useChangeSprintStatus(projectId: number, sprintId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: ChangeSprintStatusRequest) =>
      sprints.changeSprintStatus(projectId, sprintId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: sprintKeys.all(projectId) }),
  });
}

export function useDeleteSprint(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (sprintId: number) => sprints.deleteSprint(projectId, sprintId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: sprintKeys.all(projectId) }),
  });
}
