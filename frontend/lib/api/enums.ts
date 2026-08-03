/**
 * Every enum the backend exposes (docs/API.md §2). Sent and received as raw
 * uppercase strings — each mirrors a CHECK constraint in the schema.
 */

export const ROLES = ["USER", "DEVELOPER", "EDITOR", "ADMIN"] as const;
export type Role = (typeof ROLES)[number];

/** ADMIN implies EDITOR implies DEVELOPER implies USER. */
export const ROLE_RANK: Record<Role, number> = {
  USER: 0,
  DEVELOPER: 1,
  EDITOR: 2,
  ADMIN: 3,
};

export const roleAtLeast = (role: Role | undefined, min: Role): boolean =>
  !!role && ROLE_RANK[role] >= ROLE_RANK[min];

export const ROLE_LABEL: Record<Role, string> = {
  USER: "User",
  DEVELOPER: "Developer",
  EDITOR: "Editor",
  ADMIN: "Admin",
};

export const TEAM_FIELDS = [
  "BACKEND",
  "FRONTEND",
  "ANDROID",
  "IOS",
  "DESIGN",
  "DATA",
] as const;
export type TeamField = (typeof TEAM_FIELDS)[number];

export const PROJECT_STATUSES = [
  "PLANNING",
  "ACTIVE",
  "ON_HOLD",
  "COMPLETED",
  "CANCELLED",
] as const;
export type ProjectStatus = (typeof PROJECT_STATUSES)[number];

export const PROJECT_STATUS_LABEL: Record<ProjectStatus, string> = {
  PLANNING: "Planning",
  ACTIVE: "Active",
  ON_HOLD: "On hold",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

export const SPRINT_STATUSES = [
  "TODO",
  "IN_PROGRESS",
  "TESTING",
  "COMPLETED",
] as const;
export type SprintStatus = (typeof SPRINT_STATUSES)[number];

export const SPRINT_STATUS_LABEL: Record<SprintStatus, string> = {
  TODO: "To do",
  IN_PROGRESS: "In progress",
  TESTING: "Testing",
  COMPLETED: "Completed",
};

export const EPIC_STATUSES = [
  "TODO",
  "IN_PROGRESS",
  "ON_HOLD",
  "COMPLETED",
  "CANCELLED",
] as const;
export type EpicStatus = (typeof EPIC_STATUSES)[number];

export const EPIC_STATUS_LABEL: Record<EpicStatus, string> = {
  TODO: "To do",
  IN_PROGRESS: "In progress",
  ON_HOLD: "On hold",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

export const ISSUE_STATUSES = [
  "BACKLOG",
  "TODO",
  "IN_PROGRESS",
  "IN_REVIEW",
  "DONE",
  "ON_HOLD",
  "CANCELLED",
] as const;
export type IssueStatus = (typeof ISSUE_STATUSES)[number];

export const ISSUE_STATUS_LABEL: Record<IssueStatus, string> = {
  BACKLOG: "Backlog",
  TODO: "To do",
  IN_PROGRESS: "In progress",
  IN_REVIEW: "In review",
  DONE: "Done",
  ON_HOLD: "On hold",
  CANCELLED: "Cancelled",
};

/** Columns a board renders, left to right. Backlog and cancelled sit off-board. */
export const BOARD_STATUSES: IssueStatus[] = [
  "TODO",
  "IN_PROGRESS",
  "IN_REVIEW",
  "DONE",
];

export const ISSUE_TYPES = [
  "BUG",
  "FEATURE",
  "STORY",
  "TASK",
  "ENHANCEMENT",
  "REFACTOR",
] as const;
export type IssueType = (typeof ISSUE_TYPES)[number];

export const ISSUE_TYPE_LABEL: Record<IssueType, string> = {
  BUG: "Bug",
  FEATURE: "Feature",
  STORY: "Story",
  TASK: "Task",
  ENHANCEMENT: "Enhancement",
  REFACTOR: "Refactor",
};

export const ISSUE_PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const;
export type IssuePriority = (typeof ISSUE_PRIORITIES)[number];

export const ISSUE_PRIORITY_LABEL: Record<IssuePriority, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
};

export const METRICS_BUCKETS = ["DAY", "WEEK", "MONTH"] as const;
export type MetricsBucket = (typeof METRICS_BUCKETS)[number];

export const METRICS_DIMENSIONS = ["TYPE", "PRIORITY"] as const;
export type MetricsDimension = (typeof METRICS_DIMENSIONS)[number];

export const ISSUE_ACTION_TYPES = [
  "CREATED",
  "STATUS_UPDATED",
  "PRIORITY_UPDATED",
  "ASSIGNEE_USER_UPDATED",
  "ASSIGNEE_TEAM_UPDATED",
  "SPRINT_UPDATED",
  "STORY_POINT_UPDATED",
  "DETAILS_UPDATED",
  "DELETED",
] as const;
export type IssueActionType = (typeof ISSUE_ACTION_TYPES)[number];

export const SPRINT_ACTION_TYPES = [
  "CREATED",
  "STATUS_UPDATED",
  "DATES_UPDATED",
  "DETAILS_UPDATED",
  "DELETED",
] as const;
export type SprintActionType = (typeof SPRINT_ACTION_TYPES)[number];

export const PROJECT_ACTION_TYPES = [
  "CREATED",
  "LEADER_UPDATED",
  "TEAM_ADDED",
  "TEAM_REMOVED",
  "USER_ADDED",
  "USER_REMOVED",
  "DETAILS_UPDATED",
  "STATUS_UPDATED",
  "DELETED",
] as const;
export type ProjectActionType = (typeof PROJECT_ACTION_TYPES)[number];

export type ActivityActionType =
  | IssueActionType
  | SprintActionType
  | ProjectActionType;

/** Renders a raw actionType as the sentence fragment the client owns. */
export const ACTION_LABEL: Record<string, string> = {
  CREATED: "created this",
  STATUS_UPDATED: "changed status",
  PRIORITY_UPDATED: "changed priority",
  ASSIGNEE_USER_UPDATED: "changed the assignee",
  ASSIGNEE_TEAM_UPDATED: "changed the assigned team",
  SPRINT_UPDATED: "moved to a different sprint",
  STORY_POINT_UPDATED: "changed the story points",
  DETAILS_UPDATED: "updated the details",
  DELETED: "deleted this",
  DATES_UPDATED: "changed the dates",
  LEADER_UPDATED: "changed the leader",
  TEAM_ADDED: "added a team",
  TEAM_REMOVED: "removed a team",
  USER_ADDED: "added a member",
  USER_REMOVED: "removed a member",
};
