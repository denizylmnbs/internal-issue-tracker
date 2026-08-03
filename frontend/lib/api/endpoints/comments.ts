import { apiData, apiVoid, json, toQuery } from "../client";
import type {
  CommentResponse,
  CreateCommentRequest,
  PagedResponse,
  UpdateCommentRequest,
} from "../types";

const base = (projectId: number, issueId: number) =>
  `/api/projects/${projectId}/issues/${issueId}/comments`;

export const createComment = (
  projectId: number,
  issueId: number,
  body: CreateCommentRequest,
) => apiData<CommentResponse>(base(projectId, issueId), json(body, "POST"));

export const listComments = (
  projectId: number,
  issueId: number,
  query: { userId?: number; page?: number; size?: number; sort?: string } = {},
) =>
  apiData<PagedResponse<CommentResponse>>(
    `${base(projectId, issueId)}${toQuery({ sort: "createdAt,asc", size: 100, ...query })}`,
  );

export const getComment = (projectId: number, issueId: number, commentId: number) =>
  apiData<CommentResponse>(`${base(projectId, issueId)}/${commentId}`);

export const updateComment = (
  projectId: number,
  issueId: number,
  commentId: number,
  body: UpdateCommentRequest,
) =>
  apiData<CommentResponse>(`${base(projectId, issueId)}/${commentId}`, json(body, "PUT"));

export const deleteComment = (projectId: number, issueId: number, commentId: number) =>
  apiVoid(`${base(projectId, issueId)}/${commentId}`, { method: "DELETE" });
