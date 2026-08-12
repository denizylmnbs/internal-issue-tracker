/**
 * The enums that are still genuinely fixed (docs/API.md §2) — sent and
 * received as raw uppercase strings, each mirroring a CHECK constraint in
 * the schema. Status/type/priority/unit/team-field values used to live here
 * too; they are now `field_definitions` rows (user-defined, per project or
 * global) instead of a closed set — see `lib/api/types.ts`'s
 * `FieldDefinitionResponse`/`FieldKind`, `lib/project/ProjectContext.tsx`
 * (the six per-project kinds) and
 * `lib/fielddef/GlobalFieldDefinitionsProvider.tsx` (PROJECT_STATUS,
 * TEAM_FIELD).
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
