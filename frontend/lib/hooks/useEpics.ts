import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as epics from "@/lib/api/endpoints/epics";
import type { CreateEpicRequest, UpdateEpicRequest, ChangeEpicStatusRequest } from "@/lib/api/types";
import type { ListEpicsQuery } from "@/lib/api/endpoints/epics";
import { isApiClientError } from "@/lib/api/errors";

const epicKeys = {
  all: (projectId: number) => ["projects", projectId, "epics"] as const,
  list: (projectId: number, query: ListEpicsQuery) =>
    ["projects", projectId, "epics", "list", query] as const,
  detail: (projectId: number, epicId: number) => ["epics", projectId, epicId] as const,
};

export const useEpicsList = (projectId: number, query: ListEpicsQuery = {}) =>
  useQuery({
    queryKey: epicKeys.list(projectId, query),
    queryFn: () => epics.listEpics(projectId, query),
    enabled: !!projectId,
  });

export function useEpicSafe(projectId: number, epicId: number | null | undefined) {
  const query = useQuery({
    queryKey: epicKeys.detail(projectId, epicId as number),
    queryFn: () => epics.getEpic(projectId, epicId as number),
    enabled: epicId != null,
    retry: false,
  });
  const deleted = query.isError && isApiClientError(query.error) && query.error.status === 404;
  return { ...query, deleted };
}

export function useCreateEpic(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: CreateEpicRequest) => epics.createEpic(projectId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: epicKeys.all(projectId) }),
  });
}

export function useUpdateEpic(projectId: number, epicId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: UpdateEpicRequest) => epics.updateEpic(projectId, epicId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: epicKeys.all(projectId) }),
  });
}

export function useChangeEpicStatus(projectId: number, epicId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: ChangeEpicStatusRequest) => epics.changeEpicStatus(projectId, epicId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: epicKeys.all(projectId) }),
  });
}

export function useDeleteEpic(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (epicId: number) => epics.deleteEpic(projectId, epicId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: epicKeys.all(projectId) }),
  });
}
