import { keepPreviousData, useQuery } from "@tanstack/react-query";
import * as activity from "@/lib/api/endpoints/activity";

export const useIssueActivity = (projectId: number, issueId: number) =>
  useQuery({
    queryKey: ["projects", projectId, "issues", issueId, "activities"],
    queryFn: () => activity.listIssueActivity(projectId, issueId, { sort: "createdAt,asc" }),
    enabled: !!projectId && !!issueId,
  });

export const useProjectActivity = (
  projectId: number,
  query: { page?: number; size?: number } = {},
) =>
  useQuery({
    queryKey: ["projects", projectId, "activities", query],
    queryFn: () => activity.listProjectActivity(projectId, query),
    enabled: !!projectId,
    // the page number is part of the key, so without this every step through
    // the feed unmounts the list and flashes the loading state
    placeholderData: keepPreviousData,
  });

export const useSprintActivity = (projectId: number, sprintId: number) =>
  useQuery({
    queryKey: ["projects", projectId, "sprints", sprintId, "activities"],
    queryFn: () => activity.listSprintActivity(projectId, sprintId),
    enabled: !!projectId && !!sprintId,
  });
