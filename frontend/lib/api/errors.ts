import type { ApiError } from "./types";

/** Thrown by the client whenever `success: false`. Carries the HTTP status
 * alongside the envelope's error so callers can branch on either. */
export class ApiClientError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors?: { field: string; message: string }[];
  readonly traceId?: string;

  constructor(status: number, error: ApiError) {
    super(humanize(error.code) ?? error.message);
    this.name = "ApiClientError";
    this.status = status;
    this.code = error.code;
    this.fieldErrors = error.fieldErrors;
    this.traceId = error.traceId;
  }
}

/**
 * Domain and cross-cutting codes (docs/API.md §1) rewritten as sentences a
 * user can act on. Falls back to the backend's own `message` when a code
 * isn't mapped, so a new backend error code degrades gracefully rather than
 * disappearing.
 */
const MESSAGES: Record<string, string> = {
  VALIDATION_FAILED: "Some fields need attention before this can be saved.",
  MALFORMED_REQUEST: "That request could not be understood.",
  TYPE_MISMATCH: "One of the values in that request has the wrong type.",
  MISSING_PARAMETER: "A required field is missing.",
  INVALID_SORT_PROPERTY: "That sort option isn't supported here.",
  UNAUTHENTICATED: "Your session has ended. Sign in again.",
  FORBIDDEN: "You don't have permission to do that.",
  RESOURCE_NOT_FOUND: "That couldn't be found.",
  ENDPOINT_NOT_FOUND: "That page doesn't exist.",
  METHOD_NOT_ALLOWED: "That action isn't supported here.",
  CONFLICT: "That conflicts with the current state.",
  DUPLICATE_RESOURCE: "That already exists.",
  BUSINESS_RULE_VIOLATION: "That request can't be completed as it stands.",
  PAYLOAD_TOO_LARGE: "That's too large to send.",
  INTERNAL_ERROR: "Something went wrong on our side.",

  EMAIL_ALREADY_EXISTS: "That email is already registered.",
  CURRENT_PASSWORD_INCORRECT: "Current password is incorrect.",
  INVALID_CREDENTIALS: "Incorrect email or password.",
  ROLE_CHANGE_NOT_PERMITTED:
    "You can't change that person's role — you don't outrank both their current and new role.",
  TEAM_NAME_ALREADY_EXISTS: "A team with that name already exists.",
  LEADER_NOT_FOUND: "That leader couldn't be found or isn't active.",
  USER_NOT_FOUND: "That user couldn't be found.",
  USER_ROLE_NOT_ENOUGH:
    "That person needs to be at least a Developer before they can be added.",
  TEAM_NOT_FOUND: "That team couldn't be found.",
  TEAM_MEMBER_ALREADY_EXIST: "They're already a member of this team.",
  TEAM_MEMBER_NOT_FOUND: "That team membership couldn't be found.",
  PROJECT_NAME_ALREADY_EXISTS: "A project with that name already exists.",
  PROJECT_NOT_FOUND: "That project couldn't be found.",
  PROJECT_MEMBER_ALREADY_EXIST: "They're already a member of this project.",
  PROJECT_MEMBER_NOT_FOUND: "That project membership couldn't be found.",
  PROJECT_TEAM_ALREADY_EXIST: "That team is already assigned to this project.",
  PROJECT_TEAM_NOT_FOUND: "That project/team assignment couldn't be found.",
  SPRINT_NAME_ALREADY_EXISTS:
    "A sprint with that name already exists in this project.",
  SPRINT_ALREADY_IN_PROGRESS:
    "This project already has a running sprint — finish or stop it first.",
  SPRINT_NOT_FOUND: "That sprint couldn't be found.",
  EPIC_NAME_ALREADY_EXISTS:
    "An epic with that name already exists in this project.",
  EPIC_NOT_FOUND: "That epic couldn't be found.",
  ISSUE_NOT_FOUND: "That issue couldn't be found.",
  ASSIGNEE_USER_NOT_FOUND: "That assignee couldn't be found.",
  ASSIGNEE_TEAM_NOT_FOUND: "That assigned team couldn't be found.",
  COMMENT_NOT_FOUND: "That comment couldn't be found.",
  COMMENT_NOT_OWNED: "You can only edit your own comments.",
};

function humanize(code: string): string | undefined {
  return MESSAGES[code];
}

export function isApiClientError(error: unknown): error is ApiClientError {
  return error instanceof ApiClientError;
}
