import { apiData, apiVoid, json, toQuery } from "../client";
import type {
  ChangeIssueAssigneeRequest,
  ChangeIssueClassificationRequest,
  ChangeIssueEpicRequest,
  ChangeIssueSprintRequest,
  ChangeIssueStatusRequest,
  CreateIssueRequest,
  IssueListQuery,
  IssueResponse,
  PagedResponse,
  UpdateIssueRequest,
} from "../types";

export const createIssue = (projectId: number, body: CreateIssueRequest) =>
  apiData<IssueResponse>(`/api/projects/${projectId}/issues`, json(body, "POST"));

export const getIssue = (projectId: number, issueId: number) =>
  apiData<IssueResponse>(`/api/projects/${projectId}/issues/${issueId}`);

export const listIssues = (
  projectId: number,
  query: IssueListQuery & { page?: number; size?: number; sort?: string } = {},
) =>
  apiData<PagedResponse<IssueResponse>>(
    `/api/projects/${projectId}/issues${toQuery({ sort: "createdAt,desc", size: 50, ...query })}`,
  );

export const updateIssue = (
  projectId: number,
  issueId: number,
  body: UpdateIssueRequest,
) =>
  apiData<IssueResponse>(
    `/api/projects/${projectId}/issues/${issueId}`,
    json(body, "PUT"),
  );

export const changeIssueStatus = (
  projectId: number,
  issueId: number,
  body: ChangeIssueStatusRequest,
) =>
  apiData<IssueResponse>(
    `/api/projects/${projectId}/issues/${issueId}/status`,
    json(body, "PATCH"),
  );

/** The narrow alternatives to updateIssue — see docs/API.md §5. Each replaces only
 * what its path names, so moving an issue between sprints no longer means echoing
 * its description and estimate back to keep them. */
export const changeIssueSprint = (
  projectId: number,
  issueId: number,
  body: ChangeIssueSprintRequest,
) =>
  apiData<IssueResponse>(
    `/api/projects/${projectId}/issues/${issueId}/sprint`,
    json(body, "PATCH"),
  );

export const changeIssueEpic = (
  projectId: number,
  issueId: number,
  body: ChangeIssueEpicRequest,
) =>
  apiData<IssueResponse>(
    `/api/projects/${projectId}/issues/${issueId}/epic`,
    json(body, "PATCH"),
  );

export const changeIssueClassification = (
  projectId: number,
  issueId: number,
  body: ChangeIssueClassificationRequest,
) =>
  apiData<IssueResponse>(
    `/api/projects/${projectId}/issues/${issueId}/classification`,
    json(body, "PATCH"),
  );

export const changeIssueAssignee = (
  projectId: number,
  issueId: number,
  body: ChangeIssueAssigneeRequest,
) =>
  apiData<IssueResponse>(
    `/api/projects/${projectId}/issues/${issueId}/assignee`,
    json(body, "PATCH"),
  );

export const clearIssueAssignee = (projectId: number, issueId: number) =>
  apiData<IssueResponse>(`/api/projects/${projectId}/issues/${issueId}/assignee`, {
    method: "DELETE",
  });

export const deleteIssue = (projectId: number, issueId: number) =>
  apiVoid(`/api/projects/${projectId}/issues/${issueId}`, { method: "DELETE" });
