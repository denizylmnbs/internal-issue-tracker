import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "./useApiMutation";
import * as comments from "@/lib/api/endpoints/comments";
import type { CreateCommentRequest, UpdateCommentRequest } from "@/lib/api/types";

const commentKeys = {
  all: (projectId: number, issueId: number) =>
    ["projects", projectId, "issues", issueId, "comments"] as const,
};

export const useComments = (projectId: number, issueId: number) =>
  useQuery({
    queryKey: commentKeys.all(projectId, issueId),
    queryFn: () => comments.listComments(projectId, issueId, { sort: "createdAt,asc", size: 200 }),
    enabled: !!projectId && !!issueId,
  });

export function useCreateComment(projectId: number, issueId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (body: CreateCommentRequest) => comments.createComment(projectId, issueId, body),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: commentKeys.all(projectId, issueId) }),
  });
}

export function useUpdateComment(projectId: number, issueId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: ({ commentId, body }: { commentId: number; body: UpdateCommentRequest }) =>
      comments.updateComment(projectId, issueId, commentId, body),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: commentKeys.all(projectId, issueId) }),
  });
}

export function useDeleteComment(projectId: number, issueId: number) {
  const queryClient = useQueryClient();
  return useApiMutation({
    mutationFn: (commentId: number) => comments.deleteComment(projectId, issueId, commentId),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: commentKeys.all(projectId, issueId) }),
  });
}
