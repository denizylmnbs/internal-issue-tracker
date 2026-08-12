import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as fieldDefinitions from "@/lib/api/endpoints/fieldDefinitions";
import type {
  CreateFieldDefinitionRequest,
  FieldDefinitionResponse,
  FieldKind,
  ReorderFieldDefinitionsRequest,
  UpdateFieldDefinitionRequest,
} from "@/lib/api/types";

export const fieldDefinitionKeys = {
  all: ["fieldDefinitions"] as const,
  list: (projectId: number | null, kind?: FieldKind) =>
    ["fieldDefinitions", "list", projectId, kind ?? null] as const,
  /**
   * A prefix, not a query key of its own — pass this to `invalidateQueries`
   * to clear every kind's list for one project (or the global lists, for
   * `projectId: null`) in one call. TanStack Query matches by array prefix,
   * so `list(projectId)` (which pads a trailing `null` for "no kind given")
   * does *not* match a query keyed with an actual kind - `[...,
   * "ISSUE_STATUS"]` doesn't start with `[..., null]`. This shorter key,
   * one element up, does match both.
   */
  listAll: (projectId: number | null) => ["fieldDefinitions", "list", projectId] as const,
};

/** `projectId: null` fetches the two global kinds; otherwise the caller's project. */
export const useFieldDefinitionsList = (
  projectId: number | null,
  kind?: FieldKind,
  options?: { enabled?: boolean },
) =>
  useQuery({
    queryKey: fieldDefinitionKeys.list(projectId, kind),
    queryFn: () => fieldDefinitions.listFieldDefinitions(projectId, kind),
    enabled: options?.enabled,
  });

export function useCreateFieldDefinition(projectId: number | null) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: CreateFieldDefinitionRequest) =>
      fieldDefinitions.createFieldDefinition(projectId, body),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: fieldDefinitionKeys.listAll(projectId),
      }),
  });
}

export function useUpdateFieldDefinition(projectId: number | null) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: ({
      defId,
      body,
    }: {
      defId: number;
      body: UpdateFieldDefinitionRequest;
    }) => fieldDefinitions.updateFieldDefinition(projectId, defId, body),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: fieldDefinitionKeys.listAll(projectId),
      }),
  });
}

export function useReorderFieldDefinitions(projectId: number | null) {
  const queryClient = useQueryClient();
  return useApiMutation<FieldDefinitionResponse[], ReorderFieldDefinitionsRequest>({
    mutationFn: (body) =>
      fieldDefinitions.reorderFieldDefinitions(projectId, body),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: fieldDefinitionKeys.listAll(projectId),
      }),
  });
}

export function useDeleteFieldDefinition(projectId: number | null) {
  const queryClient = useQueryClient();
  return useApiMutation<void, number>({
    mutationFn: (defId) => fieldDefinitions.deleteFieldDefinition(projectId, defId),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: fieldDefinitionKeys.listAll(projectId),
      }),
  });
}
