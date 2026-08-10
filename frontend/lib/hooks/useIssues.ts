import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApiMutation } from "./useApiMutation";
import * as issues from "@/lib/api/endpoints/issues";
import type {
  CreateIssueRequest,
  UpdateIssueRequest,
  ChangeIssueStatusRequest,
  ChangeIssueAssigneeRequest,
  ChangeIssueClassificationRequest,
  ChangeIssueEpicRequest,
  ChangeIssueSprintRequest,
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

export function useChangeIssueSprint(projectId: number) {
  return useNarrowIssueEdit<ChangeIssueSprintRequest>(projectId, issues.changeIssueSprint);
}

export function useChangeIssueEpic(projectId: number) {
  return useNarrowIssueEdit<ChangeIssueEpicRequest>(projectId, issues.changeIssueEpic);
}

export function useChangeIssueClassification(projectId: number) {
  return useNarrowIssueEdit<ChangeIssueClassificationRequest>(
    projectId,
    issues.changeIssueClassification,
  );
}

/** Shaped like useChangeIssueStatus — the issue id is a variable rather than a
 * hook argument, which is what lets one hook serve a whole selection. */
function useNarrowIssueEdit<TBody>(
  projectId: number,
  call: (projectId: number, issueId: number, body: TBody) => Promise<unknown>,
) {
  const queryClient = useQueryClient();
  return useApiMutation<unknown, { issueId: number; body: TBody }>({
    mutationFn: ({ issueId, body }) => call(projectId, issueId, body),
    onSuccess: (_data, { issueId }) => {
      queryClient.invalidateQueries({ queryKey: issueKeys.all(projectId) });
      queryClient.invalidateQueries({ queryKey: issueKeys.detail(projectId, issueId) });
    },
  });
}

export type BulkIssueEdit = {
  issueIds: number[];
  /** Called once per issue. Per-issue rather than one request because the API has
   * no bulk route — see docs/API.md §5. */
  apply: (issueId: number) => Promise<unknown>;
  /** Renders the success line, e.g. `(n) => \`${n} issues moved to Sprint 4\``. */
  describe: (count: number) => string;
};

/**
 * Applies one change to a whole selection. Partial success is the normal case,
 * not an edge one — someone else may have deleted an issue, or the caller may be
 * the assignee of some of them and not others — so this settles every request,
 * reports what actually landed, and invalidates once at the end rather than N
 * times.
 */
export function useBulkIssueEdit(projectId: number) {
  const queryClient = useQueryClient();

  return useApiMutation<{ succeeded: number; failed: number }, BulkIssueEdit>({
    // the toasts below say more than a bare error message could
    silent: true,
    mutationFn: async ({ issueIds, apply }) => {
      const results = await Promise.allSettled(issueIds.map(apply));
      const rejected = results.filter((r) => r.status === "rejected");

      // nothing landed at all: rethrow so this reads as an ordinary failed
      // mutation, with useApiMutation's message and its 403 session refetch
      if (rejected.length === results.length && rejected.length > 0) {
        throw rejected[0].reason;
      }

      return { succeeded: results.length - rejected.length, failed: rejected.length };
    },
    onSuccess: ({ succeeded, failed }, { describe }) => {
      if (failed === 0) {
        toast.success(describe(succeeded));
      } else {
        toast.warning(`${describe(succeeded)} — ${failed} couldn't be changed.`);
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: issueKeys.all(projectId) }),
  });
}

export function useDeleteIssue(projectId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (issueId: number) => issues.deleteIssue(projectId, issueId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: issueKeys.all(projectId) }),
  });
}
