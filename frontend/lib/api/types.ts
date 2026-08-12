/**
 * Every DTO the backend returns or accepts (docs/API.md §4). Hand-written
 * against that document since there is no OpenAPI/springdoc in this project —
 * if the backend and this file disagree, the backend wins; fix this file.
 *
 * Status/type/priority/unit/field values (project status, sprint status, epic
 * status, issue status, issue type, issue priority, issue resolving unit,
 * team field) are plain `string` now, not fixed unions — they are
 * `field_definitions` codes (docs/API.md §2/§4.14), user-defined per project
 * or globally. See `lib/api/types.ts`'s FieldDefinition* types below and
 * `lib/project/ProjectContext.tsx` / `lib/fielddef/GlobalFieldDefinitionsProvider.tsx`
 * for how a raw code is resolved to a label/color.
 */
import type { MetricsBucket, Role, ActivityActionType } from "./enums";

// ---------------------------------------------------------------- envelope

export type ApiSuccess<T> = { success: true; data?: T; timestamp: string };
export type ApiFailure = { success: false; error: ApiError; timestamp: string };
export type ApiResponse<T> = ApiSuccess<T> | ApiFailure;

export type ApiError = {
  code: string;
  message: string;
  path?: string;
  fieldErrors?: { field: string; message: string }[];
  traceId?: string;
};

export type PageInfo = {
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type PagedResponse<T> = { content: T[]; page: PageInfo };

// -------------------------------------------------------------------- auth

export type LoginRequest = { email: string; password: string };
export type LoginResponse = { accessToken: string; refreshToken: string };

/** Same DTO backend uses for both `/api/auth/refresh` and `/api/auth/logout`. */
export type RefreshRequest = { refreshToken: string };

// ------------------------------------------------------------------- user

export type UserResponse = {
  id: number;
  name: string;
  surname: string;
  email: string;
  role: Role;
  isActive: boolean;
  createdAt: string;
};

export type RegisterRequest = {
  name: string;
  surname: string;
  email: string;
  password: string;
};

export type UpdateUserRequest = { name: string; surname: string; email: string };

export type ChangePasswordRequest = {
  currentPassword: string;
  newPassword: string;
};

export type ResetPasswordRequest = { newPassword: string };

export type ChangeRoleRequest = { newRole: Role };

export type UserTeamMembershipResponse = {
  membershipId: number;
  teamId: number;
  teamName: string;
  teamField: string | null;
  /** Derived server-side from a nullable `updated_at` — see lib/format.ts. */
  joinedAt: string | null;
};

export type UserProjectMembershipResponse = {
  projectId: number;
  projectName: string;
  projectStatus: string;
  directlyAssigned: boolean;
};

// ------------------------------------------------------------------- team

export type TeamResponse = {
  id: number;
  name: string;
  field: string | null;
  leaderId: number;
  isActive: boolean;
  createdAt: string;
};

export type CreateTeamRequest = {
  name: string;
  field?: string;
  leaderId: number;
};

export type UpdateTeamRequest = { name: string; field?: string };

export type ChangeTeamLeaderRequest = { leaderId: number };

export type TeamMemberResponse = {
  id: number;
  userId: number;
  teamId: number;
  isActive: boolean;
  /** Derived server-side from a nullable `updated_at` — see lib/format.ts. */
  joinedAt: string | null;
};

export type AddTeamMemberRequest = { userId: number };

// ---------------------------------------------------------------- project

export type ProjectResponse = {
  id: number;
  name: string;
  description: string | null;
  startDate: string;
  endDate: string | null;
  leaderId: number | null;
  status: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ProjectDetailResponse = ProjectResponse & {
  memberCount: number;
  teamCount: number;
};

export type CreateProjectRequest = {
  name: string;
  description?: string;
  startDate: string;
  endDate?: string;
  leaderId?: number;
};

export type UpdateProjectRequest = {
  name: string;
  description?: string;
  startDate: string;
  endDate?: string;
};

export type ChangeProjectLeaderRequest = { leaderId: number };
export type ChangeProjectStatusRequest = { status: string };

export type ProjectMemberResponse = {
  id: number;
  userId: number;
  projectId: number;
  isActive: boolean;
  /** Derived server-side from a nullable `updated_at` — see lib/format.ts. */
  joinedAt: string | null;
};

export type AddProjectMemberRequest = { userId: number };

export type ProjectParticipantResponse = {
  userId: number;
  directlyAssigned: boolean;
};

export type ProjectTeamResponse = {
  id: number;
  teamId: number;
  projectId: number;
  isActive: boolean;
  /** Derived server-side from a nullable `updated_at` — see lib/format.ts. */
  assignedAt: string | null;
};

export type AddProjectTeamRequest = { teamId: number };

// ----------------------------------------------------------------- sprint

export type SprintResponse = {
  id: number;
  projectId: number;
  name: string;
  description: string | null;
  startDate: string;
  endDate: string;
  status: string;
  committedPoints: number | null;
  committedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateSprintRequest = {
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
};

export type UpdateSprintRequest = {
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
};

export type ChangeSprintStatusRequest = { status: string };

// ------------------------------------------------------------------- epic

export type EpicResponse = {
  id: number;
  projectId: number;
  name: string;
  description: string | null;
  status: string;
  reporterId: number;
  createdAt: string;
  updatedAt: string;
};

export type CreateEpicRequest = { name: string; description?: string };
export type UpdateEpicRequest = { name: string; description?: string };
export type ChangeEpicStatusRequest = { status: string };

// ------------------------------------------------------------------ issue

export type IssueResponse = {
  id: number;
  projectId: number;
  sprintId: number | null;
  epicId: number | null;
  type: string;
  name: string;
  description: string | null;
  status: string;
  priority: string;
  resolvingUnit: string | null;
  storyPoint: number | null;
  reporterId: number;
  assigneeUserId: number | null;
  assigneeTeamId: number | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateIssueRequest = {
  name: string;
  description?: string;
  type: string;
  priority?: string;
  resolvingUnit?: string;
  storyPoint?: number;
  sprintId?: number;
  epicId?: number;
  assigneeUserId?: number;
  assigneeTeamId?: number;
};

/** A full replacement — every field required. Omitting sprintId clears it. */
export type UpdateIssueRequest = {
  name: string;
  description?: string;
  type: string;
  priority: string;
  resolvingUnit?: string;
  storyPoint?: number;
  sprintId?: number;
  epicId?: number;
};

export type ChangeIssueStatusRequest = { status: string };

/** null means the backlog, not "leave it alone" — the field is the whole body. */
export type ChangeIssueSprintRequest = { sprintId: number | null };

export type ChangeIssueEpicRequest = { epicId: number | null };

/** Replaced as a group, so a caller changing one restates the other two as they stand. */
export type ChangeIssueClassificationRequest = {
  type: string;
  priority: string;
  storyPoint: number | null;
};

/** Both optional and independent, but sending one alone clears the other. */
export type ChangeIssueAssigneeRequest = {
  assigneeUserId: number | null;
  assigneeTeamId: number | null;
};

export type IssueListQuery = {
  name?: string;
  type?: string;
  status?: string;
  priority?: string;
  resolvingUnit?: string;
  sprintId?: number;
  epicId?: number;
  reporterId?: number;
  assigneeUserId?: number;
  assigneeTeamId?: number;
};

/** GET /api/users/{id}/sprint-progress — see docs/API.md §4.2 for the null/semantics rules. */
export type SprintProgressEntry = {
  projectId: number;
  sprintId: number;
  sprintName: string;
  startDate: string;
  endDate: string;
  assignedPoints: number;
  completedPoints: number;
  assignedIssueCount: number;
  completedIssueCount: number;
};

export type UserSprintProgressResponse = {
  current: SprintProgressEntry[];
  previous: SprintProgressEntry[];
  /** Null when the user hasn't finished a single sprint yet — never coalesce to 0. */
  recentAveragePoints: number | null;
  recentSprintCount: number;
};

// --------------------------------------------------------------- comment

export type CommentResponse = {
  id: number;
  issueId: number;
  userId: number;
  content: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateCommentRequest = { content: string };
export type UpdateCommentRequest = { content: string };

// -------------------------------------------------------------- activity

/** `scope`/`subjectId` say which activity table a row came from and what it
 * hangs off (docs/API.md §4.12) — constant on the three single-subject
 * endpoints, but required to make sense of the unioned project feed, where
 * `id` alone isn't unique across tables. */
export type ActivityResponse = {
  id: number;
  userId: number;
  actionType: ActivityActionType;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string;
  scope: "PROJECT" | "ISSUE" | "SPRINT";
  subjectId: number;
};

// --------------------------------------------------------------- metrics

export type MetricsWindow = { from: string; to: string };

export type DurationStatsResponse = {
  window: MetricsWindow;
  issueCount: number;
  avgSeconds: number | null;
  p50Seconds: number | null;
  p85Seconds: number | null;
  p95Seconds: number | null;
};

export type ThroughputPoint = { bucketStart: string; completedCount: number };
export type ThroughputResponse = {
  window: MetricsWindow;
  bucket: MetricsBucket;
  points: ThroughputPoint[];
};

export type ThroughputBreakdownPoint = {
  bucketStart: string;
  value: string;
  completedCount: number;
  completedPoints: number;
};
export type ThroughputBreakdownResponse = {
  window: MetricsWindow;
  bucket: MetricsBucket;
  dimension: "TYPE" | "PRIORITY";
  points: ThroughputBreakdownPoint[];
};

export type TimeInStatusEntry = {
  status: string;
  issueCount: number;
  totalSeconds: number;
  p50Seconds: number;
};
export type TimeInStatusResponse = {
  window: MetricsWindow;
  entries: TimeInStatusEntry[];
};

export type FlowEfficiencyResponse = {
  window: MetricsWindow;
  flowEfficiency: number;
  activeSeconds: number;
  totalSeconds: number;
};

export type ReopenRateResponse = {
  window: MetricsWindow;
  doneIssueCount: number;
  reopenedIssueCount: number;
  reopenRate: number;
};

export type NetFlowPoint = {
  bucketStart: string;
  createdCount: number;
  completedCount: number;
  netCount: number;
  cumulativeNetCount: number;
};
export type NetFlowResponse = {
  window: MetricsWindow;
  bucket: MetricsBucket;
  points: NetFlowPoint[];
};

export type DefectRatioPoint = {
  bucketStart: string;
  createdCount: number;
  createdBugCount: number;
  createdBugShare: number;
  completedCount: number;
  completedBugCount: number;
  completedStoryPoints: number;
  defectsPerCompletedIssue: number | null;
  defectsPerCompletedPoint: number | null;
};
export type DefectRatioResponse = {
  window: MetricsWindow;
  bucket: MetricsBucket;
  points: DefectRatioPoint[];
};

export type WipStatusEntry = {
  status: string;
  issueCount: number;
  storyPoints: number;
  oldestAgeSeconds: number;
  p50AgeSeconds: number;
};
export type WipOldestEntry = {
  issueId: number;
  status: string;
  enteredAt: string;
  ageSeconds: number;
  storyPoint: number | null;
  type: string;
  priority: string;
};
export type WipResponse = {
  asOf: string;
  byStatus: WipStatusEntry[];
  oldest: WipOldestEntry[];
};

export type VelocitySprintEntry = {
  sprintId: number;
  name: string;
  status: string;
  startDate: string;
  endDate: string;
  committedPoints: number | null;
  completedPoints: number | null;
  completedIssueCount: number;
  sayDoRatio: number | null;
};
export type VelocityResponse = { sprints: VelocitySprintEntry[] };

export type BurndownPoint = {
  bucketStart: string;
  remainingPoints: number;
  remainingIssueCount: number;
  completedPoints: number;
  scopePoints: number;
};
export type BurndownResponse = {
  sprintId: number;
  name: string;
  status: string;
  startDate: string;
  endDate: string;
  committedPoints: number | null;
  points: BurndownPoint[];
};

export type CumulativeFlowPoint = {
  bucketStart: string;
  status: string;
  issueCount: number;
};
export type CumulativeFlowResponse = {
  window: MetricsWindow;
  points: CumulativeFlowPoint[];
};

// ------------------------------------------------------------ field definitions

/**
 * The eight classification points every status/type/priority/unit/field code
 * belongs to (docs/API.md §2). Fixed — unlike the codes themselves, this set
 * doesn't change. `PROJECT_STATUS` and `TEAM_FIELD` are global; the other six
 * are project-scoped.
 */
export const FIELD_KINDS = [
  "PROJECT_STATUS",
  "SPRINT_STATUS",
  "EPIC_STATUS",
  "ISSUE_STATUS",
  "ISSUE_TYPE",
  "ISSUE_PRIORITY",
  "ISSUE_UNIT",
  "TEAM_FIELD",
] as const;
export type FieldKind = (typeof FIELD_KINDS)[number];

export const GLOBAL_FIELD_KINDS: FieldKind[] = ["PROJECT_STATUS", "TEAM_FIELD"];

/**
 * One user-defined status/type/priority/unit/field value (docs/API.md §4.14).
 * `projectId` is null for a global-kind row. `code` is immutable once
 * created; everything else (`label`, `color`, `sortOrder`, the five flags)
 * can change without touching any issue/sprint/epic/project already carrying
 * this code. A soft-deleted (`isActive: false`) row can still be referenced
 * by old data — callers resolving a code should fall back to showing the raw
 * code when no matching row is found, not assume one always exists.
 */
export type FieldDefinitionResponse = {
  id: number;
  kind: FieldKind;
  projectId: number | null;
  code: string;
  label: string;
  color: string | null;
  sortOrder: number;
  isActive: boolean;
  isDefault: boolean;
  isDone: boolean;
  isCancelled: boolean;
  isActiveWork: boolean;
  isDefect: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateFieldDefinitionRequest = {
  kind: FieldKind;
  code: string;
  label: string;
  color?: string;
  isDefault: boolean;
  isDone: boolean;
  isCancelled: boolean;
  isActiveWork: boolean;
  isDefect: boolean;
};

/** No `code` — it never changes once created, see FieldDefinitionResponse. */
export type UpdateFieldDefinitionRequest = {
  label: string;
  color?: string;
  isDefault: boolean;
  isDone: boolean;
  isCancelled: boolean;
  isActiveWork: boolean;
  isDefect: boolean;
};

/** `orderedIds` must name exactly the active rows of this kind — no more, no fewer. */
export type ReorderFieldDefinitionsRequest = {
  kind: FieldKind;
  orderedIds: number[];
};
