import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as issues from "@/lib/api/endpoints/issues";
import type {
  CreateIssueRequest,
  UpdateIssueRequest,
  ChangeIssueStatusRequest,
  ChangeIssueAssigneeRequest,
  IssueListQuery,
} from "@/lib/api/types";

const issueKeys = {
  all: (projectId: number) => ["projects", projectId, "issues"] as const,
  list: (projectId: number, query: IssueListQuery) =>
    ["projects", projectId, "issues", "list", query] as const,
  detail: (projectId: number, issueId: number) =>
    ["projects", projectId, "issues", issueId] as const,
};

export const useIssuesList = (
  projectId: number,
  query: IssueListQuery & { size?: number; sort?: string } = {},
) =>
  useQuery({
    queryKey: issueKeys.list(projectId, query),
    queryFn: () => issues.listIssues(projectId, query),
    enabled: !!projectId,
  });

export const useIssue = (projectId: number, issueId: number) =>
  useQuery({
    queryKey: issueKeys.detail(projectId, issueId),
    queryFn: () => issues.getIssue(projectId, issueId),
    enabled: !!projectId && !!issueId,
  });

export function useCreateIssue(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: CreateIssueRequest) => issues.createIssue(projectId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: issueKeys.all(projectId) }),
  });
}

export function useUpdateIssue(projectId: number, issueId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: UpdateIssueRequest) => issues.updateIssue(projectId, issueId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: issueKeys.all(projectId) });
      queryClient.invalidateQueries({ queryKey: issueKeys.detail(projectId, issueId) });
    },
  });
}

/** Optimistic — this is the drag-and-drop board's mutation, and a round trip
 * before the card visibly moves would defeat the point of dragging it. */
export function useChangeIssueStatus(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation<
    Awaited<ReturnType<typeof issues.changeIssueStatus>>,
    { issueId: number; body: ChangeIssueStatusRequest }
  >({
    mutationFn: ({ issueId, body }) => issues.changeIssueStatus(projectId, issueId, body),
    onMutate: async ({ issueId, body }) => {
      await queryClient.cancelQueries({ queryKey: issueKeys.all(projectId) });
      const previous = queryClient.getQueriesData({ queryKey: issueKeys.all(projectId) });
      queryClient.setQueriesData({ queryKey: issueKeys.all(projectId) }, (old: any) => {
        if (!old?.content) return old;
        return {
          ...old,
          content: old.content.map((i: any) =>
            i.id === issueId ? { ...i, status: body.status } : i,
          ),
        };
      });
      return { previous };
    },
    onError: (_err, _vars, context) => {
      (context as { previous?: [readonly unknown[], unknown][] } | undefined)?.previous?.forEach(
        ([key, data]) => queryClient.setQueryData(key, data),
      );
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: issueKeys.all(projectId) });
    },
  });
}

export function useChangeIssueAssignee(projectId: number, issueId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: ChangeIssueAssigneeRequest) =>
      issues.changeIssueAssignee(projectId, issueId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: issueKeys.all(projectId) });
      queryClient.invalidateQueries({ queryKey: issueKeys.detail(projectId, issueId) });
    },
  });
}

export function useClearIssueAssignee(projectId: number, issueId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: () => issues.clearIssueAssignee(projectId, issueId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: issueKeys.all(projectId) });
      queryClient.invalidateQueries({ queryKey: issueKeys.detail(projectId, issueId) });
    },
  });
}

export function useDeleteIssue(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (issueId: number) => issues.deleteIssue(projectId, issueId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: issueKeys.all(projectId) }),
  });
}
