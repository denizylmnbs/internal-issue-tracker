import { roleAtLeast } from "@/lib/api/enums";
import type { Role } from "@/lib/api/enums";

/**
 * Pure predicates mirroring the three project-scoped rule shapes in
 * docs/API.md §3 (`editorOrTeamLeader`, `editorOrProjectLeader`,
 * `editorLeaderOrParticipant`). These decide what the UI *offers* — every
 * mutation still goes through the backend's own check, which is the one that
 * actually matters (docs/API.md §5 note 8: roles are re-read from the
 * database on every request, never trust a cached one for anything
 * security-relevant). Hiding an affordance here is a courtesy, not a gate.
 */

export type Caller = { id: number; role: Role } | undefined;

export const isAdmin = (caller: Caller) => roleAtLeast(caller?.role, "ADMIN");
export const isEditorOrAbove = (caller: Caller) => roleAtLeast(caller?.role, "EDITOR");
export const isDeveloperOrAbove = (caller: Caller) =>
  roleAtLeast(caller?.role, "DEVELOPER");

export const editorOrTeamLeader = (caller: Caller, teamLeaderId: number | undefined) =>
  isEditorOrAbove(caller) || (!!caller && caller.id === teamLeaderId);

export const editorOrProjectLeader = (
  caller: Caller,
  projectLeaderId: number | null | undefined,
) => isEditorOrAbove(caller) || (!!caller && caller.id === projectLeaderId);

export const editorLeaderOrParticipant = (
  caller: Caller,
  projectLeaderId: number | null | undefined,
  participantIds: Set<number> | undefined,
) =>
  editorOrProjectLeader(caller, projectLeaderId) ||
  (!!caller && !!participantIds?.has(caller.id));

/** Can this person be added to a team or project? (docs/API.md §3 — a plain
 * USER cannot be, and yields 403 USER_ROLE_NOT_ENOUGH.) */
export const isEligibleForMembership = (role: Role) => roleAtLeast(role, "DEVELOPER");

/** Author-only, even for an admin (docs/API.md §4.11 — 403 COMMENT_NOT_OWNED
 * is deliberate, not a 404, since the caller can already see the comment). */
export const canEditComment = (caller: Caller, commentUserId: number) =>
  !!caller && caller.id === commentUserId;

/** Wider than editing: the author, an EDITOR, or the project's leader. */
export const canDeleteComment = (
  caller: Caller,
  commentUserId: number,
  projectLeaderId: number | null | undefined,
) =>
  canEditComment(caller, commentUserId) || editorOrProjectLeader(caller, projectLeaderId);

/** Deleting an issue is the one operation participants don't get. */
export const canDeleteIssue = editorOrProjectLeader;

/**
 * Narrower than {@link editorLeaderOrParticipant}: status changes and
 * assignee changes (docs/API.md §4.10) are editor / leader / **the issue's
 * own assignee** — being a project participant alone is not enough. Mirrors
 * `IssueService#requireEditorLeaderOrAssignee` on the backend, which is the
 * check that actually matters here.
 */
export const canWriteIssue = (
  caller: Caller,
  projectLeaderId: number | null | undefined,
  issue: { assigneeUserId: number | null } | undefined,
) =>
  editorOrProjectLeader(caller, projectLeaderId) ||
  (!!caller && !!issue && caller.id === issue.assigneeUserId);
