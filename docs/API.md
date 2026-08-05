# Internal Issue Tracker — API Reference

Complete reference for every HTTP endpoint the backend exposes, written to be the single source a
frontend is built against. **81 endpoints across 16 controllers.**

Generated from the source. When the code and this document disagree, the code in
`src/main/java/com/ist/internal_issue_tracker/**/*Controller.java` wins — but please fix the document.

> **CORS is configured** for `http://localhost:3000` by default (override with the
> `CORS_ALLOWED_ORIGINS` env var, comma-separated). A Next.js dev server can call this API directly.

---

## Table of contents

- [1. Conventions](#1-conventions) — envelope, auth, pagination, errors
- [2. Enum reference](#2-enum-reference)
- [3. Authorization model](#3-authorization-model)
- [4. Endpoints](#4-endpoints)
  - [4.1 Auth](#41-auth) · [4.2 Users](#42-users) · [4.3 Teams](#43-teams) · [4.4 Team members](#44-team-members)
  - [4.5 Projects](#45-projects) · [4.6 Project members](#46-project-members) · [4.7 Project teams](#47-project-teams)
  - [4.8 Sprints](#48-sprints) · [4.9 Epics](#49-epics) · [4.10 Issues](#410-issues) · [4.11 Comments](#411-comments)
  - [4.12 Activity log](#412-activity-log) · [4.13 Metrics](#413-metrics)
- [5. Frontend integration notes](#5-frontend-integration-notes)
- [6. Known gaps](#6-known-gaps-read-before-starting-the-frontend)

---

## 1. Conventions

### Base URL

```
http://localhost:8080
```

Every route is prefixed with `/api`. There is no version segment in the path.

### Response envelope

**Every** controller returns the same wrapper (`shared/web/ApiResponse.java`). Exactly one of `data`
and `error` is populated, and `null` fields are omitted from the JSON entirely
(`@JsonInclude(NON_NULL)`).

Success:

```json
{
  "success": true,
  "data": { "id": 1, "name": "Deniz" },
  "timestamp": "2026-08-01T10:15:30.123Z"
}
```

Endpoints with no meaningful payload (all `DELETE`s, `reset-password`) return `success: true` with
**no `data` key at all** — not `"data": null`.

Failure:

```json
{
  "success": false,
  "error": {
    "code": "PROJECT_NOT_FOUND",
    "message": "This project does not exist.",
    "path": "/api/projects/42",
    "fieldErrors": [{ "field": "name", "message": "Name cannot be blank" }],
    "traceId": "a1b2c3d4"
  },
  "timestamp": "2026-08-01T10:15:30.123Z"
}
```

The HTTP status line is the single source of truth for the status — it is deliberately **not**
duplicated inside the body. `fieldErrors` appears only on `VALIDATION_FAILED`; `traceId` only on
`INTERNAL_ERROR`.

A suggested client unwrapper:

```ts
type ApiResponse<T> =
  | { success: true; data?: T; timestamp: string }
  | { success: false; error: ApiError; timestamp: string };

type ApiError = {
  code: string;
  message: string;
  path?: string;
  fieldErrors?: { field: string; message: string }[];
  traceId?: string;
};
```

### Paged responses

List endpoints return `ApiResponse<PagedResponse<T>>` (`shared/web/PagedResponse.java`) — a stable
shape this application owns rather than Spring Data's `Page` serialization:

```json
{
  "success": true,
  "data": {
    "content": [ /* T[] */ ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 137,
      "totalPages": 7,
      "first": true,
      "last": false
    }
  },
  "timestamp": "2026-08-01T10:15:30.123Z"
}
```

Every paged endpoint accepts Spring's standard `Pageable` query parameters:

| Param  | Default | Notes                                                                 |
|--------|---------|-----------------------------------------------------------------------|
| `page` | `0`     | Zero-based.                                                           |
| `size` | `20`    | Spring Boot's default; not overridden in `application.properties`.    |
| `sort` | none    | `sort=createdAt,desc`. Repeat the param for multi-key sorting.        |

`sort` takes **entity property names**, not response field names (they coincide almost everywhere).
An unknown property yields `400 INVALID_SORT_PROPERTY`.

### Authentication

Stateless JWT bearer tokens. Send on every request except the public routes:

```
Authorization: Bearer <accessToken>
```

- The token's only claim is `sub` = the user's **id** (`shared/security/JwtService.java`). It carries
  **no role, no email, no name** — call [`GET /api/auth/me`](#41-auth) for those.
- The role is resolved from the database on every single request
  (`UserAuthenticatedUserLookup`), so a role change or a deactivation takes effect immediately rather
  than at token expiry. Never cache a role client-side beyond a page's lifetime.
- Lifetime comes from the `JWT_EXPIRATION` env var (milliseconds).
- An invalid, expired, or malformed token does not 401 in the filter — it simply leaves the request
  unauthenticated, and the authorization rules reject it afterwards.
- **There is a refresh endpoint now.** `POST /api/auth/login` returns an `accessToken` *and* a
  `refreshToken`. The refresh token is an opaque random string, not a JWT — it is meaningless to the
  client beyond "hand it back to `/api/auth/refresh`". It is tracked server-side in Redis
  (`auth/RefreshTokenService.java`), which is what makes it revocable, unlike the access token.
  - `POST /api/auth/refresh` exchanges a refresh token for a **new** access/refresh pair and
    invalidates the one you sent — treat every refresh token as single-use. Store whichever one the
    response gives you back; the old string stops working immediately.
  - `POST /api/auth/logout` revokes a refresh token so it can no longer be exchanged. It does **not**
    invalidate the current access token — that one simply expires on its own, so still stop sending
    it client-side and drop it from memory on logout.
  - The server also revokes **every** refresh token a user holds (all devices/tabs) when their
    password changes or their account is deactivated. A refresh call after that returns
    `401 INVALID_REFRESH_TOKEN` — treat it the same as an expired session and send the user back to
    login.
  - Refresh token lifetime comes from the `JWT_REFRESH_EXPIRATION` env var (milliseconds).

### Errors

All framework and domain exceptions funnel through `GlobalExceptionHandler` into the envelope above.

**Auth-specific codes** (`auth/exception/AuthErrorCode.java`):

| Code                     | Status | Meaning                                                    |
|--------------------------|--------|-------------------------------------------------------------|
| `INVALID_REFRESH_TOKEN`  | 401    | Refresh token is unknown, expired, already used, or revoked. |

**Cross-cutting codes** (`shared/exception/CommonErrorCode.java`):

| Code                     | Status | Meaning                                       |
|--------------------------|--------|-----------------------------------------------|
| `VALIDATION_FAILED`      | 400    | Bean-validation failure; see `fieldErrors`.   |
| `MALFORMED_REQUEST`      | 400    | Body could not be parsed.                     |
| `TYPE_MISMATCH`          | 400    | A query/path param has the wrong type.        |
| `MISSING_PARAMETER`      | 400    | A required query param is absent.             |
| `INVALID_SORT_PROPERTY`  | 400    | `sort` names a property that does not exist.  |
| `UNAUTHENTICATED`        | 401    | No/invalid token on a protected route.        |
| `FORBIDDEN`              | 403    | Authenticated, but not allowed here.          |
| `RESOURCE_NOT_FOUND`     | 404    | Generic missing resource.                     |
| `ENDPOINT_NOT_FOUND`     | 404    | No route matches.                             |
| `METHOD_NOT_ALLOWED`     | 405    | Wrong verb; `Allow` header is set.            |
| `NOT_ACCEPTABLE`         | 406    |                                               |
| `UNSUPPORTED_MEDIA_TYPE` | 415    |                                               |
| `CONFLICT`               | 409    | Conflicts with current state.                 |
| `DUPLICATE_RESOURCE`     | 409    |                                               |
| `BUSINESS_RULE_VIOLATION`| 422    |                                               |
| `PAYLOAD_TOO_LARGE`      | 413    |                                               |
| `INTERNAL_ERROR`         | 500    | Message is generic; quote `traceId` to support.|

**Domain codes**, by module. Note the deliberate `422`-vs-`404` split: `404` means *the thing you
addressed* does not exist; `422` means the request is well-formed and its subject exists, but
something it *points at* cannot be used.

| Code | Status | Module |
|------|--------|--------|
| `EMAIL_ALREADY_EXISTS` | 409 | user |
| `CURRENT_PASSWORD_INCORRECT` | 400 | user |
| `INVALID_CREDENTIALS` | 401 | user / auth |
| `ROLE_CHANGE_NOT_PERMITTED` | 403 | user |
| `TEAM_NAME_ALREADY_EXISTS` | 409 | team |
| `LEADER_NOT_FOUND` | 422 | team, project |
| `USER_NOT_FOUND` | 422 | team member, project member |
| `USER_ROLE_NOT_ENOUGH` | 403 | team member, project member |
| `TEAM_NOT_FOUND` | 422 | team member, project team |
| `TEAM_MEMBER_ALREADY_EXIST` | 409 | team member |
| `TEAM_MEMBER_NOT_FOUND` | 404 | team member |
| `PROJECT_NAME_ALREADY_EXISTS` | 409 | project |
| `PROJECT_NOT_FOUND` | 404 | project, sprint, epic, issue, comment, activity |
| `PROJECT_MEMBER_ALREADY_EXIST` | 409 | project member |
| `PROJECT_MEMBER_NOT_FOUND` | 404 | project member |
| `PROJECT_TEAM_ALREADY_EXIST` | 409 | project team |
| `PROJECT_TEAM_NOT_FOUND` | 404 | project team |
| `SPRINT_NAME_ALREADY_EXISTS` | 409 | sprint |
| `SPRINT_ALREADY_IN_PROGRESS` | 409 | sprint |
| `SPRINT_NOT_FOUND` | 404 (sprint, activity) / 422 (issue) | — |
| `EPIC_NAME_ALREADY_EXISTS` | 409 | epic |
| `EPIC_NOT_FOUND` | 404 (epic) / 422 (issue) | — |
| `ISSUE_NOT_FOUND` | 404 | issue, comment, activity |
| `ASSIGNEE_USER_NOT_FOUND` | 422 | issue |
| `ASSIGNEE_TEAM_NOT_FOUND` | 422 | issue |
| `COMMENT_NOT_FOUND` | 404 | comment |
| `COMMENT_NOT_OWNED` | 403 | comment |

### Date & time formats

| Type | Format | Example | Used for |
|------|--------|---------|----------|
| `LocalDate` | `yyyy-MM-dd` | `2026-08-01` | project/sprint start & end dates |
| `OffsetDateTime` | ISO-8601 with offset | `2026-08-01T10:15:30Z` | all timestamps, all metric windows |

The database session is pinned to UTC (`spring.datasource.hikari.connection-init-sql`), so metric
buckets are labelled in UTC regardless of server locale. Render in the user's zone client-side.

### Soft deletes

Almost nothing is hard-deleted. Users, teams, project memberships and team memberships carry
`isActive`; sprints, epics, issues and comments carry a `deletedAt` stamp that is **never exposed** —
response DTOs for those simply never contain a deleted row. A `DELETE` returns `200` with an empty
envelope, not `204`.

Re-adding a removed member revives the original row rather than inserting a new one, which is why
`joinedAt`/`assignedAt` mean *most recent* join, not first ever.

---

## 2. Enum reference

Send and expect these as raw uppercase strings. Every one mirrors a `CHECK` constraint in the schema.

| Enum | Values |
|------|--------|
| `Role` | `USER`, `DEVELOPER`, `EDITOR`, `ADMIN` |
| `TeamField` | `BACKEND`, `FRONTEND`, `ANDROID`, `IOS`, `DESIGN`, `DATA` |
| `ProjectStatus` | `PLANNING`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, `CANCELLED` |
| `SprintStatus` | `TODO`, `IN_PROGRESS`, `TESTING`, `COMPLETED` |
| `EpicStatus` | `TODO`, `IN_PROGRESS`, `ON_HOLD`, `COMPLETED`, `CANCELLED` |
| `IssueStatus` | `BACKLOG`, `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`, `ON_HOLD`, `CANCELLED` |
| `IssueType` | `BUG`, `FEATURE`, `STORY`, `TASK`, `ENHANCEMENT`, `REFACTOR` |
| `IssuePriority` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `MetricsBucket` | `DAY`, `WEEK`, `MONTH` |
| `MetricsDimension` | `TYPE`, `PRIORITY` |

**Activity action types** — returned as `actionType` strings on activity rows. Rendering them as
sentences is the client's job; that is why the API returns the raw name.

| Log | Values |
|-----|--------|
| Issue | `CREATED`, `STATUS_UPDATED`, `PRIORITY_UPDATED`, `ASSIGNEE_USER_UPDATED`, `ASSIGNEE_TEAM_UPDATED`, `SPRINT_UPDATED`, `STORY_POINT_UPDATED`, `DETAILS_UPDATED`, `DELETED` |
| Sprint | `CREATED`, `STATUS_UPDATED`, `DATES_UPDATED`, `DETAILS_UPDATED`, `DELETED` |
| Project | `CREATED`, `LEADER_UPDATED`, `TEAM_ADDED`, `TEAM_REMOVED`, `USER_ADDED`, `USER_REMOVED`, `DETAILS_UPDATED`, `STATUS_UPDATED`, `DELETED` |

Lifecycle rules worth encoding in the UI:

- **Nothing is created in a chosen status.** Projects are born `PLANNING`, sprints and epics `TODO`,
  issues `BACKLOG`. No create request accepts a status field — status moves only through its own
  `PATCH .../status` endpoint. Do not render a status picker on any create form.
- `IssuePriority` **is** accepted at creation (filing something `CRITICAL` from the start is
  ordinary); omitting it takes the `MEDIUM` default.
- `CANCELLED` is a decision, not a deletion. Deleting is a separate `DELETE`.

---

## 3. Authorization model

Two independent axes, both checked. Rules live centrally in
`shared/security/SecurityConfig.java`, not in annotations.

### Global role hierarchy

```
ADMIN → EDITOR → DEVELOPER → USER
```

Expanded at decision time by Spring's `RoleHierarchy` bean, so a rule requiring `EDITOR` also admits
`ADMIN`. Each endpoint below states its **minimum** role.

### Project-scoped access

"Project leader" is a relationship stored in `projects.leader_id`, not a role. Three rule shapes
appear in the table below:

| Shorthand | Means |
|-----------|-------|
| **public** | No token needed. |
| **auth** | Any authenticated user. |
| **self+admin** | The user themself, or an `ADMIN`. |
| **EDITOR** | Role `EDITOR` or above. |
| **ADMIN** | Role `ADMIN`. |
| **editor / team-leader** | `EDITOR`+, or the leader of *this* team. |
| **editor / leader** | `EDITOR`+, or the leader of *this* project. |
| **editor / leader / participant** | `EDITOR`+, the project's leader, or anyone who works on it — a direct member **or** a member of an assigned team. |

Everything not listed in `SecurityConfig` falls through to `anyRequest().authenticated()`. Reads are
therefore open to any logged-in user **except** the activity feeds and the metrics, which are held to
the same participation rule as writing.

### Two membership rules that surprise people

- Adding a user to a **team** or a **project** requires that user to already hold at least
  `DEVELOPER` globally. A plain `USER` cannot be added and yields `403 USER_ROLE_NOT_ENOUGH`. Filter
  the people-picker accordingly.
- Changing a role (`PATCH /api/users/{id}/role`) needs `ADMIN` at the gate, and then the service
  additionally requires the caller to **strictly outrank both** the target's current role and the new
  role. So an `ADMIN` cannot promote anyone to `ADMIN`, and cannot change another `ADMIN`, and cannot
  change their own role. Otherwise `403 ROLE_CHANGE_NOT_PERMITTED`.

---

## 4. Endpoints

### 4.1 Auth

`auth/AuthController.java`

#### `POST /api/auth/login` — **public**

Request:
```json
{ "email": "deniz@example.com", "password": "hunter2000" }
```
`email` must be a valid, non-blank email; `password` non-blank.

Response `200`:
```json
{
  "success": true,
  "data": { "accessToken": "eyJhbGciOi...", "refreshToken": "k3F9pQZ..." },
  "timestamp": "..."
}
```

Errors: `401 INVALID_CREDENTIALS` (same message for a wrong password and an unknown email — do not
leak which), `400 VALIDATION_FAILED`.

`accessToken`'s only claim is the user's id. Follow it with `GET /api/auth/me` to learn who logged
in. `refreshToken` is opaque — store it, don't decode it — and use it against `/api/auth/refresh`
once the access token expires.

#### `POST /api/auth/refresh` — **public**

Request:
```json
{ "refreshToken": "k3F9pQZ..." }
```

Response `200`: same shape as login — a brand-new `accessToken` **and** `refreshToken`. The refresh
token you sent is consumed; discard it and store the new one.

```json
{
  "success": true,
  "data": { "accessToken": "eyJhbGciOi...", "refreshToken": "n8LpXw..." },
  "timestamp": "..."
}
```

Errors: `401 INVALID_REFRESH_TOKEN` (unknown, expired, already-used, or revoked — e.g. after a
password change), `400 VALIDATION_FAILED`. On `401`, send the user back to login; there is no third
token to fall back to.

#### `POST /api/auth/logout` — **public**

Request:
```json
{ "refreshToken": "k3F9pQZ..." }
```

Response `200`: empty envelope (`{ "success": true, "data": null, "timestamp": "..." }`). Revokes the
refresh token server-side; already-invalid tokens are accepted silently (idempotent). Does **not**
invalidate the current access token — drop it client-side too, it just expires on its own.

Errors: `400 VALIDATION_FAILED`.

#### `GET /api/auth/me` — **auth**

The current user, resolved from the bearer token alone. Returns the same `UserResponse` shape as
`GET /api/users/{id}`, so a client has one type for "a user" rather than a special case for itself:

```json
{
  "success": true,
  "data": {
    "id": 1, "name": "Deniz", "surname": "Yalmanbas",
    "email": "deniz@example.com", "role": "DEVELOPER",
    "isActive": true, "createdAt": "2026-07-24T12:00:00Z"
  },
  "timestamp": "..."
}
```

This is the call to make on app load and after login — **`role` is what every piece of conditional UI
depends on**, and it is not in the token. Errors: `401 UNAUTHENTICATED`.

> Implemented in `user/CurrentUserController.java`, not in the `auth` module: `auth` is declared
> `allowedDependencies = "shared"` and may not return a `user` type. The URL says nothing about which
> module owns the handler — the same arrangement as `/api/users/{id}/teams`, which lives in `team`.

---

### 4.2 Users

`user/UserController.java`, plus two user-centric listings that live in other modules.

| Method | Path | Access |
|--------|------|--------|
| `POST` | `/api/users/register` | **public** |
| `GET` | `/api/users/{id}` | auth |
| `GET` | `/api/users` | auth |
| `PUT` | `/api/users/{id}` | self+admin |
| `DELETE` | `/api/users/{id}` | ADMIN |
| `PATCH` | `/api/users/{id}/password` | self+admin |
| `POST` | `/api/users/{id}/reset-password` | ADMIN |
| `PATCH` | `/api/users/{id}/role` | ADMIN (+ outranking rule) |
| `GET` | `/api/users/{id}/teams` | auth |
| `GET` | `/api/users/{id}/projects` | auth |

**`UserResponse`** — returned by every endpoint in this section that returns a user:

```json
{
  "id": 1,
  "name": "Deniz",
  "surname": "Yalmanbas",
  "email": "deniz@example.com",
  "role": "DEVELOPER",
  "isActive": true,
  "createdAt": "2026-07-24T12:00:00Z"
}
```

The password hash is never present in any response.

#### `POST /api/users/register` → `201`, `Location: /api/users/{id}`

```json
{ "name": "Deniz", "surname": "Yalmanbas", "email": "Deniz@Example.com ", "password": "hunter2000" }
```
`name`/`surname` 2–255 chars, `email` valid, `password` ≥ 8 chars. The email is **normalized**
(trimmed and lowercased) inside the DTO's compact constructor, so `Deniz@Example.com ` and
`deniz@example.com` collide.

Always creates a `USER`. There is no role field, by design — promotion is a separate admin action.

Errors: `409 EMAIL_ALREADY_EXISTS`, `400 VALIDATION_FAILED`.

#### `GET /api/users/{id}` → `200` `UserResponse`

#### `GET /api/users` → `200` `PagedResponse<UserResponse>`

Query: `name`, `surname` (both optional, partial match), plus `page`/`size`/`sort`.
There is no email or free-text search, and no "active only" filter — `isActive` comes back on each
row and is filtered client-side.

#### `PUT /api/users/{id}` → `200` `UserResponse`

```json
{ "name": "Deniz", "surname": "Yalmanbas", "email": "deniz@example.com" }
```
Full replacement of the three editable fields — all required. Role, password and `isActive` are each
their own operation. Email is normalized as on register. Errors: `409 EMAIL_ALREADY_EXISTS`.

#### `DELETE /api/users/{id}` → `200` empty

Soft delete: sets `isActive = false`. Cascades through `UserDeactivatedEvent` — the user is dropped
from team memberships and project assignments by listeners.

#### `PATCH /api/users/{id}/password` → `200` `UserResponse`

```json
{ "currentPassword": "hunter2000", "newPassword": "correcthorse" }
```
`newPassword` ≥ 8 chars. Errors: `400 CURRENT_PASSWORD_INCORRECT`.

#### `POST /api/users/{id}/reset-password` → `200` empty

```json
{ "newPassword": "correcthorse" }
```
Admin override — no current password required.

#### `PATCH /api/users/{id}/role` → `200` `UserResponse`

```json
{ "newRole": "DEVELOPER" }
```
See the outranking rule in [§3](#3-authorization-model). Errors: `403 ROLE_CHANGE_NOT_PERMITTED`.

#### `GET /api/users/{id}/teams` → `200` `PagedResponse<UserTeamMembershipResponse>`

Which teams this user belongs to. Only **active** memberships, so no `isActive` field.

```json
{
  "membershipId": 12,
  "teamId": 3,
  "teamName": "Platform",
  "teamField": "BACKEND",
  "joinedAt": "2026-07-25T09:00:00Z"
}
```

Carries enough of the team to render a list without an N+1 call per row.

#### `GET /api/users/{id}/projects` → `200` `PagedResponse<UserProjectMembershipResponse>`

Which projects this user works on, **by either route**.

```json
{
  "projectId": 7,
  "projectName": "Checkout revamp",
  "projectStatus": "ACTIVE",
  "directlyAssigned": true
}
```

`directlyAssigned: false` means they are here through an assigned team, so removing them from *this
project* is not the operation you want.

> This is the endpoint to build the logged-in user's "My projects" home page from.

---

### 4.3 Teams

`team/TeamController.java`

| Method | Path | Access |
|--------|------|--------|
| `POST` | `/api/teams` | EDITOR |
| `GET` | `/api/teams/{id}` | auth |
| `GET` | `/api/teams` | auth |
| `PUT` | `/api/teams/{id}` | editor / team-leader |
| `PATCH` | `/api/teams/{id}/leader` | EDITOR |
| `DELETE` | `/api/teams/{id}` | EDITOR |

**`TeamResponse`**:
```json
{ "id": 3, "name": "Platform", "field": "BACKEND", "leaderId": 1, "isActive": true, "createdAt": "..." }
```

#### `POST /api/teams` → `201`, `Location: /api/teams/{id}`
```json
{ "name": "Platform", "field": "BACKEND", "leaderId": 1 }
```
`name` 2–255 and globally unique; `field` optional; `leaderId` **required** and must name an active
user. Errors: `409 TEAM_NAME_ALREADY_EXISTS`, `422 LEADER_NOT_FOUND`.

#### `GET /api/teams` → `200` `PagedResponse<TeamResponse>`
Query: `name`, `field` (`TeamField`), `leaderId`. All optional.

#### `PUT /api/teams/{id}` → `200`
```json
{ "name": "Platform", "field": "BACKEND" }
```
The leader is deliberately absent — changing it is its own, `EDITOR`-only operation.

#### `PATCH /api/teams/{id}/leader` → `200`
```json
{ "leaderId": 4 }
```
Errors: `422 LEADER_NOT_FOUND`. Unlike a project, a team has **no** remove-leader endpoint.

#### `DELETE /api/teams/{id}` → `200` empty
Soft delete (`isActive = false`). Fires `TeamDeactivatedEvent`, which cleans up the team's project
assignments.

---

### 4.4 Team members

`team/TeamMemberController.java` and `team/UserTeamsController.java`

| Method | Path | Access |
|--------|------|--------|
| `POST` | `/api/teams/{id}/members` | editor / team-leader |
| `GET` | `/api/teams/{id}/members` | auth |
| `GET` | `/api/teams/members` | auth |
| `DELETE` | `/api/teams/{id}/members/{userId}` | editor / team-leader |

**`TeamMemberResponse`**:
```json
{ "id": 12, "userId": 5, "teamId": 3, "isActive": true, "joinedAt": "2026-07-25T09:00:00Z" }
```
`joinedAt` is the **most recent** join, not the first ever.

#### `POST /api/teams/{id}/members` → `201`, `Location: /api/teams/{id}/members/{userId}`
```json
{ "userId": 5 }
```
The team comes from the path, never the body — the authorization rule has to know the team before the
controller runs, and it can only read path variables.

Errors: `422 USER_NOT_FOUND`, `422 TEAM_NOT_FOUND`, `403 USER_ROLE_NOT_ENOUGH` (target is below
`DEVELOPER`), `409 TEAM_MEMBER_ALREADY_EXIST`.

#### `GET /api/teams/members` → `200` `PagedResponse<TeamMemberResponse>`
Every membership in the system, across all teams. The literal `/members` segment outranks
`/api/teams/{id}` in Spring's path matching, so this does not collide with "get team 
with id `members`".

#### `DELETE /api/teams/{id}/members/{userId}` → `200` empty
Soft delete, addressed by the `(team, user)` pair rather than the membership's surrogate id.
Errors: `404 TEAM_MEMBER_NOT_FOUND`.

---

### 4.5 Projects

`project/ProjectController.java`

| Method | Path | Access |
|--------|------|--------|
| `POST` | `/api/projects` | EDITOR |
| `GET` | `/api/projects/{id}` | auth |
| `GET` | `/api/projects` | auth |
| `PUT` | `/api/projects/{id}` | editor / leader |
| `PATCH` | `/api/projects/{id}/leader` | EDITOR |
| `DELETE` | `/api/projects/{id}/leader` | EDITOR |
| `PATCH` | `/api/projects/{id}/status` | editor / leader |
| `DELETE` | `/api/projects/{id}` | EDITOR |

**`ProjectResponse`** (list rows):
```json
{
  "id": 7, "name": "Checkout revamp", "description": "…",
  "startDate": "2026-08-01", "endDate": "2026-12-31",
  "leaderId": 1, "status": "ACTIVE", "isActive": true,
  "createdAt": "…", "updatedAt": "…"
}
```

**`ProjectDetailResponse`** (single project) — the same fields **plus**:
```json
{ "memberCount": 9, "teamCount": 2 }
```
`memberCount` counts everyone who works on the project — direct assignments plus everyone reached
through an assigned team, deduplicated. It matches `GET /{id}/participants`, **not**
`GET /{id}/members`. The counts are kept off the list endpoint so listing does not run two extra
counts per row.

#### `POST /api/projects` → `201`, `Location: /api/projects/{id}`
```json
{
  "name": "Checkout revamp",
  "description": "optional",
  "startDate": "2026-08-01",
  "endDate": "2026-12-31",
  "leaderId": 1
}
```
`name` 2–255, unique. `startDate` required. `endDate` optional but must not precede `startDate` —
that cross-field rule surfaces as a `fieldErrors` entry keyed on the object name, not a field.
`leaderId` optional (a project can be opened now and staffed later); when given it must be an active
user. The status is absent by design — every project starts `PLANNING`.

Errors: `409 PROJECT_NAME_ALREADY_EXISTS`, `422 LEADER_NOT_FOUND`.

#### `GET /api/projects` → `200` `PagedResponse<ProjectResponse>`

Query, all optional: `name`, `status` (`ProjectStatus`), `leaderId`, `startDateAfter` (`yyyy-MM-dd`),
`endDateBefore` (`yyyy-MM-dd`).

#### `PUT /api/projects/{id}` → `200` `ProjectResponse`
```json
{ "name": "…", "description": "…", "startDate": "2026-08-01", "endDate": "2026-12-31" }
```
Leader and status are each their own operation.

#### `PATCH /api/projects/{id}/leader` → `200` — `{ "leaderId": 4 }`
#### `DELETE /api/projects/{id}/leader` → `200` `ProjectResponse`
Leaves the project with **no** leader. Until a new one is named, only an `EDITOR` can act on it —
worth warning about in the UI before confirming.

#### `PATCH /api/projects/{id}/status` → `200` — `{ "status": "ACTIVE" }`
#### `DELETE /api/projects/{id}` → `200` empty
Soft delete. A soft-deleted project accepts no new members, sprints, epics or issues.

---

### 4.6 Project members

`project/ProjectMemberController.java` and `project/UserProjectsController.java`

| Method | Path | Access |
|--------|------|--------|
| `POST` | `/api/projects/{id}/members` | editor / leader |
| `GET` | `/api/projects/{id}/members` | auth |
| `GET` | `/api/projects/{id}/participants` | auth |
| `DELETE` | `/api/projects/{id}/members/{userId}` | editor / leader |

**Two different lists, and picking the wrong one is the classic bug here:**

- **`/members`** — the *direct assignment rows* only. This is the set `POST` and `DELETE` act on.
- **`/participants`** — *everyone who works on the project*, including people reached through an
  assigned team. This is what `memberCount` on the project detail counts, and it is what you show on
  an "Assignees" picker.

**`ProjectMemberResponse`**:
```json
{ "id": 30, "userId": 5, "projectId": 7, "isActive": true, "joinedAt": "…" }
```

**`ProjectParticipantResponse`**:
```json
{ "userId": 5, "directlyAssigned": true }
```
Just the id and the route — resolve names via `GET /api/users/{id}`. When `directlyAssigned` is
`false`, removing that person means removing the *team*, or removing them from that team; the
`DELETE` below will return `404 PROJECT_MEMBER_NOT_FOUND`.

#### `POST /api/projects/{id}/members` → `201` — `{ "userId": 5 }`
Errors: `422 USER_NOT_FOUND`, `403 USER_ROLE_NOT_ENOUGH` (below `DEVELOPER`),
`409 PROJECT_MEMBER_ALREADY_EXIST`, `404 PROJECT_NOT_FOUND`.

#### `DELETE /api/projects/{id}/members/{userId}` → `200` empty
Errors: `404 PROJECT_MEMBER_NOT_FOUND`.

---

### 4.7 Project teams

`project/ProjectTeamController.java`

| Method | Path | Access |
|--------|------|--------|
| `POST` | `/api/projects/{id}/teams` | editor / leader |
| `GET` | `/api/projects/{id}/teams` | auth |
| `DELETE` | `/api/projects/{id}/teams/{teamId}` | editor / leader |

**`ProjectTeamResponse`**:
```json
{ "id": 8, "teamId": 3, "projectId": 7, "isActive": true, "assignedAt": "…" }
```

#### `POST /api/projects/{id}/teams` → `201` — `{ "teamId": 3 }`
Errors: `422 TEAM_NOT_FOUND`, `409 PROJECT_TEAM_ALREADY_EXIST`.

#### `DELETE /api/projects/{id}/teams/{teamId}` → `200` empty
Errors: `404 PROJECT_TEAM_NOT_FOUND`. Removing a team also removes every participant who was on the
project only through it — `memberCount` will drop by more than one.

---

### 4.8 Sprints

`sprint/SprintController.java` — all under `/api/projects/{id}/sprints`

| Method | Path | Access |
|--------|------|--------|
| `POST` | `/api/projects/{id}/sprints` | editor / leader |
| `GET` | `/api/projects/{id}/sprints` | auth |
| `GET` | `/api/projects/{id}/sprints/{sprintId}` | auth |
| `PUT` | `/api/projects/{id}/sprints/{sprintId}` | editor / leader |
| `PATCH` | `/api/projects/{id}/sprints/{sprintId}/status` | editor / leader |
| `DELETE` | `/api/projects/{id}/sprints/{sprintId}` | editor / leader |

**`SprintResponse`**:
```json
{
  "id": 4, "projectId": 7, "name": "Sprint 12", "description": "…",
  "startDate": "2026-08-01", "endDate": "2026-08-15",
  "status": "IN_PROGRESS",
  "committedPoints": 34,
  "committedAt": "2026-08-01T09:00:00Z",
  "createdAt": "…", "updatedAt": "…"
}
```

`committedPoints` / `committedAt` are **read-only** — no request DTO carries them. They are stamped
exactly once, automatically, at the moment the sprint is moved to `IN_PROGRESS`, and record the sum
of story points in the sprint at that instant. Both are `null` before the sprint starts, and stay
`null` forever on a sprint that was started before the column existed. This is the `committedPoints`
the velocity and burndown metrics read.

#### `POST` → `201`
```json
{ "name": "Sprint 12", "description": "…", "startDate": "2026-08-01", "endDate": "2026-08-15" }
```
`name` 2–255, unique **within the project**. Status is absent by design — starting a sprint straight
from creation would slip past the one-running-sprint check.

Errors: `409 SPRINT_NAME_ALREADY_EXISTS`, `404 PROJECT_NOT_FOUND`, `400 VALIDATION_FAILED`.

#### `GET` → `200` `PagedResponse<SprintResponse>`
Query: `name`, `status` (`SprintStatus`), both optional.

#### `PATCH .../status` → `200` — `{ "status": "IN_PROGRESS" }`

**A project may have only one sprint `IN_PROGRESS` at a time**, enforced by a partial unique index
(`one_active_sprint_per_project`) as well as a check. Attempting a second yields
`409 SPRINT_ALREADY_IN_PROGRESS` — surface this clearly, it is the most likely conflict a sprint board
will hit.

#### `DELETE` → `200` empty
Soft delete. **It does not cascade**: issues that pointed at this sprint keep their `sprintId`, and a
client following it gets a `404`. Treat a `sprintId` on an issue as possibly dangling.

---

### 4.9 Epics

`epic/EpicController.java` — all under `/api/projects/{id}/epics`

| Method | Path | Access |
|--------|------|--------|
| `POST` | `/api/projects/{id}/epics` | editor / leader |
| `GET` | `/api/projects/{id}/epics` | auth |
| `GET` | `/api/projects/{id}/epics/{epicId}` | auth |
| `PUT` | `/api/projects/{id}/epics/{epicId}` | editor / leader |
| `PATCH` | `/api/projects/{id}/epics/{epicId}/status` | editor / leader |
| `DELETE` | `/api/projects/{id}/epics/{epicId}` | editor / leader |

**`EpicResponse`**:
```json
{
  "id": 2, "projectId": 7, "name": "Payments", "description": "…",
  "status": "IN_PROGRESS", "reporterId": 1,
  "createdAt": "…", "updatedAt": "…"
}
```

#### `POST` → `201` — `{ "name": "Payments", "description": "…" }`
The reporter is the authenticated caller and is never accepted from the body. Name unique within the
project. Errors: `409 EPIC_NAME_ALREADY_EXISTS`.

#### `GET` → `200` `PagedResponse<EpicResponse>`
Query: `name`, `status` (`EpicStatus`), `reporterId`.

#### `PUT` → `200` — `{ "name": "…", "description": "…" }`
#### `PATCH .../status` → `200` — `{ "status": "COMPLETED" }`
#### `DELETE` → `200` empty — soft delete, does not cascade to issues (see sprints).

---

### 4.10 Issues

`issue/IssueController.java` — all under `/api/projects/{id}/issues`. The busiest resource, and the
first that opens writes to ordinary project participants.

| Method | Path | Access |
|--------|------|--------|
| `POST` | `/api/projects/{id}/issues` | editor / leader / **participant** |
| `GET` | `/api/projects/{id}/issues` | auth |
| `GET` | `/api/projects/{id}/issues/{issueId}` | auth |
| `PUT` | `/api/projects/{id}/issues/{issueId}` | editor / leader / **participant** |
| `PATCH` | `/api/projects/{id}/issues/{issueId}/status` | editor / leader / **participant** |
| `PATCH` | `/api/projects/{id}/issues/{issueId}/assignee` | editor / leader / **participant** |
| `DELETE` | `/api/projects/{id}/issues/{issueId}/assignee` | editor / leader / **participant** |
| `DELETE` | `/api/projects/{id}/issues/{issueId}` | editor / leader — **not** participants |

Deleting is the one issue operation a participant does not get.

**`IssueResponse`**:
```json
{
  "id": 101, "projectId": 7, "sprintId": 4, "epicId": 2,
  "type": "BUG", "name": "Checkout 500s on retry", "description": "…",
  "status": "IN_PROGRESS", "priority": "CRITICAL", "storyPoint": 3,
  "reporterId": 1, "assigneeUserId": 5, "assigneeTeamId": 3,
  "createdAt": "…", "updatedAt": "…"
}
```

**The two assignees are independent, not alternatives.** An issue can be handed to a team and then
picked up by a person on that team, in which case both are set. Either may be null on its own.

#### `POST` → `201`, `Location: /api/projects/{id}/issues/{issueId}`
```json
{
  "name": "Checkout 500s on retry",
  "description": "…",
  "type": "BUG",
  "priority": "CRITICAL",
  "storyPoint": 3,
  "sprintId": 4,
  "epicId": 2,
  "assigneeUserId": 5,
  "assigneeTeamId": 3
}
```
`name` 2–255 (**not** unique — two issues may share a name), `type` required, `priority` optional
(defaults `MEDIUM`), `storyPoint` ≥ 0 and optional. `sprintId`/`epicId` optional but must belong to
*this* project. Reporter comes from the caller.

Errors: `422 SPRINT_NOT_FOUND`, `422 EPIC_NOT_FOUND`, `422 ASSIGNEE_USER_NOT_FOUND`,
`422 ASSIGNEE_TEAM_NOT_FOUND`, `404 PROJECT_NOT_FOUND`.

#### `GET` → `200` `PagedResponse<IssueResponse>`

Every filter optional and combinable — together they cover what a board asks without an endpoint each:

| Param | Type |
|-------|------|
| `name` | string, partial match |
| `type` | `IssueType` |
| `status` | `IssueStatus` |
| `priority` | `IssuePriority` |
| `sprintId` | int |
| `epicId` | int |
| `reporterId` | int |
| `assigneeUserId` | int |
| `assigneeTeamId` | int |

Typical uses: a sprint board is `?sprintId=4&size=200`; "my work" is
`?assigneeUserId=<me>&status=IN_PROGRESS`; a backlog is `?status=BACKLOG&sort=priority,desc`.

#### `PUT /{issueId}` → `200`
```json
{
  "name": "…", "description": "…", "type": "BUG", "priority": "HIGH",
  "storyPoint": 5, "sprintId": 4, "epicId": 2
}
```
**A full replacement, not a patch** — and that is what makes the nullable fields unambiguous. Omitting
`sprintId` **removes the issue from its sprint**. Always send the current values for fields the form
did not touch.

Status and assignees are deliberately *not* here: they have their own endpoints so the two things a
board changes one at a time cannot be clobbered by someone saving an edit form at the same moment.
The reporter is not editable at all.

#### `PATCH /{issueId}/status` → `200` — `{ "status": "IN_REVIEW" }`
The endpoint a drag-and-drop board calls. Any transition is allowed; there is no state machine.

#### `PATCH /{issueId}/assignee` → `200`
```json
{ "assigneeUserId": 5, "assigneeTeamId": 3 }
```
Both optional and independent. Sending only a team hands the work to that team; sending both says a
named person on that team has it. **Sending one alone clears the other** — this is a replacement of
the assignment as a whole, so send both fields whenever you mean to keep both.

#### `DELETE /{issueId}/assignee` → `200` `IssueResponse` — clears **both**.
#### `DELETE /{issueId}` → `200` empty — soft delete.

---

### 4.11 Comments

`comment/CommentController.java` — under `/api/projects/{id}/issues/{issueId}/comments`

| Method | Path | Access |
|--------|------|--------|
| `POST` | `…/comments` | editor / leader / participant |
| `GET` | `…/comments` | auth |
| `GET` | `…/comments/{commentId}` | auth |
| `PUT` | `…/comments/{commentId}` | gate + **author only** |
| `DELETE` | `…/comments/{commentId}` | gate + author, `EDITOR`, or project leader |

The route gate is the coarse check; *who* may touch a given comment is decided in `CommentService`.

**`CommentResponse`**:
```json
{
  "id": 55, "issueId": 101, "userId": 5, "content": "Reproduced on staging.",
  "createdAt": "2026-08-01T10:00:00Z", "updatedAt": "2026-08-01T10:04:00Z"
}
```
`updatedAt > createdAt` is the **only** signal that a comment was edited after it was written —
render an "edited" marker from it.

#### `POST` → `201` — `{ "content": "…" }`
One field: the issue comes from the path and the author from the token, so neither can be forged.
`content` non-blank, ≤ 5000 chars.

#### `GET` → `200` `PagedResponse<CommentResponse>`
Query: `userId` (optional) to filter by author. Add `sort=createdAt,asc` for a conversation thread —
there is no default ordering.

#### `PUT /{commentId}` → `200`
`{ "content": "…" }`. **Author only**, even for an admin: `403 COMMENT_NOT_OWNED` otherwise. The
`403`-rather-than-`404` is deliberate — the caller can already see the comment in the listing, so
hiding it would fool nobody.

#### `DELETE /{commentId}` → `200` empty
Wider than editing: the author, an `EDITOR`, or the project's leader.

---

### 4.12 Activity log

`activity/ActivityController.java` — under `/api/projects/{id}`

| Method | Path | Access |
|--------|------|--------|
| `GET` | `/api/projects/{id}/activities` | editor / leader / participant |
| `GET` | `/api/projects/{id}/issues/{issueId}/activities` | editor / leader / participant |
| `GET` | `/api/projects/{id}/sprints/{sprintId}/activities` | editor / leader / participant |

**Read-only by design, not by omission** — a row is written by an event listener or not at all, so
there is no `POST`, `PUT` or `DELETE` to leave out. These are among the only reads restricted beyond
being logged in: a feed of who changed what and when is held to the same participation rule as
writing.

All three return `PagedResponse<ActivityResponse>` in one shared shape:

```json
{
  "id": 900,
  "userId": 5,
  "actionType": "STATUS_UPDATED",
  "oldValue": "TODO",
  "newValue": "IN_PROGRESS",
  "createdAt": "2026-08-01T10:00:00Z"
}
```

Reading these correctly:

- `actionType` is the **raw enum name**, per-log (see [§2](#2-enum-reference)). Turning
  `ASSIGNEE_USER_UPDATED` into a sentence is the client's job — which is exactly what lets the wording
  change without a new API version.
- `userId` is the **actor**, as an id rather than a name. Resolve it via `GET /api/users/{id}` and
  cache it; a name copied into a history row would go stale the moment it is edited.
- `oldValue` / `newValue` are strings, and **may both be null** on a `DETAILS_UPDATED` row. Name,
  description and type share one action type, and only a *name* change renders values — a description
  does not fit in 255 characters, and truncating it would produce something that looks like a value
  but is not. So "the details moved, without saying to what" is a legitimate, expected row. Render it
  as such rather than as `null → null`.
- Values are ids for the reference fields (`SPRINT_UPDATED`, `ASSIGNEE_*_UPDATED`) and enum names for
  the enum fields. `CREATED` and `DELETED` bound the entity's life and carry no values.
- An update that restates every field with the value it already had produces **no row at all** — the
  log deliberately does not record non-events.
- One call that moves several fields lands as several rows, one per field.

---

### 4.13 Metrics

`activity/metrics/IssueMetricsController.java` — 14 endpoints under `/api/projects/{id}/metrics`,
all `GET`, all **editor / leader / participant**.

Every metric is computed from the activity log, not from the issues' current state — which is what
makes them historical rather than a snapshot.

**Shared conventions:**

- `from` / `to` are optional `OffsetDateTime` (ISO-8601, e.g. `2026-05-01T00:00:00Z`) and default to
  **the last 90 days** ending now.
- The window is **half-open** — `from` inclusive, `to` exclusive — so consecutive windows tile
  without double-counting a boundary event.
- Every windowed response echoes the resolved window back as
  `"window": { "from": "…", "to": "…" }`. A metric without its window is not a metric; render the
  period alongside the number.
- `bucket` is `DAY` | `WEEK` | `MONTH`, defaulting to `WEEK` where it applies.
- **Nothing here is paged.** Each returns a single row or one point per bucket over a bounded window
  — a shape a client charts whole rather than scrolls.
- **Empty buckets are absent, not zero.** Series are sparse; a client drawing a continuous axis
  supplies its own baseline.
- All durations are **seconds** (`Double`), left for the client to render. A server that chose between
  "3 days" and "76 hours" would have made a presentation decision on the client's behalf.
- These are **project-level aggregates only**. There is no per-person breakdown, deliberately: a
  metric that becomes an individual performance measure stops describing the work and starts
  describing how people respond to being measured — which would corrupt the very log it is computed
  from. `MetricsDimension` has no `ASSIGNEE` for the same reason, all the way down to the column.

| Path | Params | Shape |
|------|--------|-------|
| `/cycle-time` | `from`, `to` | `DurationStatsResponse` |
| `/lead-time` | `from`, `to` | `DurationStatsResponse` |
| `/bug-mttr` | `from`, `to` | `DurationStatsResponse` |
| `/throughput` | `bucket`, `from`, `to` | `ThroughputResponse` |
| `/throughput-breakdown` | `dimension`, `bucket`, `from`, `to` | `ThroughputBreakdownResponse` |
| `/time-in-status` | `from`, `to` | `TimeInStatusResponse` |
| `/flow-efficiency` | `from`, `to` | `FlowEfficiencyResponse` |
| `/reopen-rate` | `from`, `to` | `ReopenRateResponse` |
| `/net-flow` | `bucket`, `from`, `to` | `NetFlowResponse` |
| `/defect-ratio` | `bucket`, `from`, `to` | `DefectRatioResponse` |
| `/wip` | `asOf` | `WipResponse` |
| `/velocity` | *none* | `VelocityResponse` |
| `/burndown` | `sprintId` **(required)** | `BurndownResponse` |
| `/cfd` | `from`, `to` | `CumulativeFlowResponse` |

#### Duration metrics — `/cycle-time`, `/lead-time`, `/bug-mttr`

```json
{
  "window": { "from": "…", "to": "…" },
  "issueCount": 42,
  "avgSeconds": 234000.0,
  "p50Seconds": 180000.0,
  "p85Seconds": 410000.0,
  "p95Seconds": 620000.0
}
```

- **cycle-time** — first `IN_PROGRESS` → first `DONE`. How long work takes *once started*.
- **lead-time** — `CREATED` → first `DONE`. How long work takes *from being asked for*.
- **bug-mttr** — the same, for bugs only, measured from when the bug was reported rather than picked up.

Every duration is `null` when `issueCount` is `0` — deliberately not zeroed, because "no data" and
"instant" are different claims. **`p85` is the number worth quoting**; the average hides the tail
that people actually feel.

#### `/throughput` — issues completed per bucket

```json
{
  "window": {…}, "bucket": "WEEK",
  "points": [{ "bucketStart": "2026-07-27T00:00:00Z", "completedCount": 12 }]
}
```
`CANCELLED` is **not** counted as completed — otherwise a team could improve its numbers by
cancelling work.

#### `/throughput-breakdown` — throughput split by `TYPE` or `PRIORITY`

```json
{
  "window": {…}, "bucket": "WEEK", "dimension": "TYPE",
  "points": [
    { "bucketStart": "…", "value": "BUG", "completedCount": 4, "completedPoints": 9 }
  ]
}
```
A **sparse matrix, not a grid** — a bucket where no bug was completed has no `BUG` row at all. Stack
these client-side against your own zero baseline. The dimension is echoed back because the request may
have taken the default.

#### `/time-in-status` — where the time went

```json
{
  "window": {…},
  "entries": [
    { "status": "IN_REVIEW", "issueCount": 30, "totalSeconds": 900000.0, "p50Seconds": 21000.0 }
  ]
}
```
Read across the entries, this is the query that **locates a queue** — the status with a large total
against a small count is where work waits.

#### `/flow-efficiency` — the worked share of elapsed time

```json
{ "window": {…}, "flowEfficiency": 0.31, "activeSeconds": 300000.0, "totalSeconds": 960000.0 }
```
A fraction between 0 and 1. "Active" means `IN_PROGRESS` or `IN_REVIEW` — that choice *is* the metric's
only real parameter, and a team that counts review as waiting reads a very different number. The two
totals come back so the ratio can be checked rather than taken on trust.

#### `/reopen-rate` — what share of finished work came back

```json
{ "window": {…}, "doneIssueCount": 40, "reopenedIssueCount": 3, "reopenRate": 0.075 }
```

#### `/net-flow` — arrivals against departures

```json
{
  "window": {…}, "bucket": "WEEK",
  "points": [{
    "bucketStart": "…", "createdCount": 14, "completedCount": 12,
    "netCount": 2, "cumulativeNetCount": 9
  }]
}
```
Whether the pile is growing, regardless of how fast it moves. `cumulativeNetCount` is the backlog
trend — the line to chart.

#### `/defect-ratio` — bug share and defect density

```json
{
  "window": {…}, "bucket": "WEEK",
  "points": [{
    "bucketStart": "…",
    "createdCount": 20, "createdBugCount": 6, "createdBugShare": 0.3,
    "completedCount": 15, "completedBugCount": 5, "completedStoryPoints": 40,
    "defectsPerCompletedIssue": 0.4, "defectsPerCompletedPoint": 0.15
  }]
}
```
Both denominators are present because neither alone is enough: bug *share* moves when total intake
moves, and density-per-point moves when estimates drift.

#### `/wip` — what is on the board right now

Takes **`asOf`** (optional, defaults to now) rather than a window: work in progress is a *level*, not
a flow. It is the one metric here that reports the present. Passing a past instant asks what the board
looked like then, which is what makes a screenshot of this reproducible.

```json
{
  "asOf": "2026-08-01T10:00:00Z",
  "byStatus": [{
    "status": "IN_PROGRESS", "issueCount": 8, "storyPoints": 21,
    "oldestAgeSeconds": 1900000.0, "p50AgeSeconds": 240000.0
  }],
  "oldest": [{
    "issueId": 101, "status": "IN_PROGRESS", "enteredAt": "…",
    "ageSeconds": 1900000.0, "storyPoint": 3, "type": "BUG", "priority": "CRITICAL"
  }]
}
```
Two views of the same set: `byStatus` is the aggregate a team watches for a trend, `oldest` is the
short list it acts on — and the only place in these metrics where an individual issue is named. Link
each `issueId` straight to the issue page.

#### `/velocity` — committed against delivered, per sprint

**No window and no parameters**: a sprint is its own window, and the series is every sprint the
project has run, in order.

```json
{
  "sprints": [{
    "sprintId": 4, "name": "Sprint 12", "status": "COMPLETED",
    "startDate": "2026-08-01", "endDate": "2026-08-15",
    "committedPoints": 34, "completedPoints": 30,
    "completedIssueCount": 11, "sayDoRatio": 0.88
  }]
}
```
- Sprints not yet started are **included, with nulls** — that is how a client charts the plan
  alongside the history.
- `committedPoints` may be `null` while `completedPoints` is not (a sprint predating the commitment
  column, or one whose work finished without the sprint ever being started). `sayDoRatio` is then
  `null` too, rather than being invented from the delivered figure — which would make every such
  sprint look like a perfect hit. **Do not coalesce it to 1.0.**
- A ratio **above 1.0 is not an error and not necessarily good**: more was delivered than committed,
  usually scope pulled in mid-sprint, sometimes an under-ambitious plan.

#### `/burndown` — one sprint, day by day

`?sprintId=4` is **required**; this is the only metric that takes a mandatory parameter. No window —
the dates come from the sprint.

```json
{
  "sprintId": 4, "name": "Sprint 12", "status": "IN_PROGRESS",
  "startDate": "2026-08-01", "endDate": "2026-08-15",
  "committedPoints": 34,
  "points": [{
    "bucketStart": "2026-08-01T00:00:00Z",
    "remainingPoints": 34, "remainingIssueCount": 11,
    "completedPoints": 0, "scopePoints": 34
  }]
}
```
The sprint's own figures come back alongside the series because the chart is unreadable without them:
**the ideal line is drawn client-side**, from `committedPoints` at `startDate` down to zero at
`endDate`. The series stops at the sprint's end date *or today, whichever is earlier*, so a running
sprint does not trail a flat line into the future. `scopePoints` against `committedPoints` is where
scope creep shows.

#### `/cfd` — cumulative flow diagram

```json
{
  "window": {…},
  "points": [{ "bucketStart": "2026-07-27T00:00:00Z", "status": "IN_PROGRESS", "issueCount": 8 }]
}
```
One row per day per **occupied** status. Bucketing is **fixed at a day** and there is no `bucket`
parameter — a CFD is read for the width of its bands over time, and a weekly cut smooths away the
queue forming that is the only thing it is drawn to show.

---

## 5. Frontend integration notes

### Endpoint totals

| Module | Count |
|--------|-------|
| Auth (`login`, `me`) | 2 |
| Users (incl. `/{id}/teams`, `/{id}/projects`) | 10 |
| Teams + team members | 10 |
| Projects | 8 |
| Project members | 4 |
| Project teams | 3 |
| Sprints | 6 |
| Epics | 6 |
| Issues | 8 |
| Comments | 5 |
| Activity | 3 |
| Metrics | 14 |
| **Total** | **79** |

### Suggested route → endpoint mapping

| Page | Calls |
|------|-------|
| `/login` | `POST /api/auth/login`, then `GET /api/auth/me` |
| `/register` | `POST /api/users/register` |
| *(app shell / session)* | `GET /api/auth/me` |
| `/` (my work) | `GET /api/users/{me}/projects`, `GET /api/users/{me}/teams` |
| `/projects` | `GET /api/projects` |
| `/projects/{id}` | `GET /api/projects/{id}`, `GET /api/projects/{id}/sprints?status=IN_PROGRESS` |
| `/projects/{id}/board` | `GET /api/projects/{id}/issues?sprintId=…`, `PATCH …/issues/{issueId}/status` |
| `/projects/{id}/backlog` | `GET /api/projects/{id}/issues?status=BACKLOG` |
| `/projects/{id}/issues/{issueId}` | issue + `…/comments` + `…/activities` |
| `/projects/{id}/sprints` | `GET/POST/PATCH …/sprints` |
| `/projects/{id}/epics` | `GET/POST/PATCH …/epics` |
| `/projects/{id}/settings` | `…/members`, `…/participants`, `…/teams`, `PATCH …/leader` |
| `/projects/{id}/insights` | the 14 metric endpoints |
| `/projects/{id}/activity` | `GET /api/projects/{id}/activities` |
| `/teams`, `/teams/{id}` | `/api/teams`, `/api/teams/{id}/members` |
| `/admin/users` | `GET /api/users`, `PATCH …/role`, `DELETE …` |

### Things that will bite

1. **Ids only, never names.** `reporterId`, `assigneeUserId`, `leaderId`, activity `userId`,
   `ProjectParticipantResponse.userId` — all bare integers. There is **no batch user-lookup
   endpoint**. Fetch `GET /api/users?size=200` once and keep an id→user map in a client cache
   (TanStack Query, a context, whatever) rather than firing `GET /api/users/{id}` per row.
2. **`PUT` is a full replacement everywhere.** Omitting a field clears it. Most damaging on
   `PUT .../issues/{issueId}`, where a missing `sprintId` silently pulls the issue out of its sprint.
   Prefill edit forms from the current entity and always send every field.
3. **`PATCH .../assignee` replaces the pair.** Send both fields whenever you mean to keep both.
4. **Soft deletes do not cascade to references.** An issue's `sprintId` or `epicId` can point at a
   deleted sprint/epic; following it returns `404`. Degrade gracefully instead of erroring the page.
5. **`/members` ≠ `/participants`** on a project. Use `/participants` for pickers and counts,
   `/members` for the add/remove UI. See [§4.6](#46-project-members).
6. **Only one sprint may be `IN_PROGRESS` per project** — `409 SPRINT_ALREADY_IN_PROGRESS`.
7. **Adding to a team or project requires the target to be `DEVELOPER`+** — filter the picker, or
   handle `403 USER_ROLE_NOT_ENOUGH`.
8. **Roles are re-read from the database on every request.** Do not trust a cached role for anything
   security-relevant; render optimistically and let a `403` correct you.
9. **`data` is absent, not null, on empty successes.** `response.data.data` is `undefined` after a
   `DELETE`. Type it optional.
10. **Metrics series are sparse** — supply your own zero baseline before charting.
11. **No default sort** on any list endpoint. Pass `sort=` explicitly or the order is the database's
    choice and may change between calls.

### Suggested client skeleton

```ts
// lib/api.ts
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  });

  const body = (await res.json()) as ApiResponse<T>;
  if (!body.success) throw new ApiClientError(res.status, body.error);
  return body.data as T;   // undefined for empty successes — type accordingly
}
```

---

## 6. Known gaps — read before starting the frontend

### 6.1 CORS — **resolved**

`SecurityConfig` now declares a `CorsConfigurationSource` for `/api/**`, wired into the filter chain
ahead of the authorization rules so an `OPTIONS` preflight — which carries no `Authorization`
header — is answered rather than rejected.

| Setting | Value |
|---------|-------|
| Origins | `app.cors.allowed-origins`, default `http://localhost:3000`; override with the `CORS_ALLOWED_ORIGINS` env var (comma-separated) |
| Methods | `GET, POST, PUT, PATCH, DELETE, OPTIONS` |
| Request headers | `Authorization`, `Content-Type`, `Accept` |
| Exposed headers | `Location` (so a client can read the URL of what it just created) |
| Credentials | allowed |
| Preflight cache | 1 hour |

Origins are an explicit list rather than `*`, which `allowCredentials` forbids anyway. **Add the
deployed frontend's origin to `CORS_ALLOWED_ORIGINS` before shipping** — a missing entry fails the
same way the original gap did.

Proxying through Next.js `rewrites()` remains a valid alternative and is still the better option if
you want the token in an httpOnly cookie rather than in JS-readable storage. The two do not conflict.

### 6.2 `GET /api/auth/me` — **resolved**

Implemented in `user/CurrentUserController.java`; documented in [§4.1](#41-auth). One call returns the
authenticated caller's full `UserResponse`, including the **role**, so no client needs to decode the
JWT.

### 6.3 Refresh tokens — **resolved**

`POST /api/auth/login` now returns a `refreshToken` alongside the `accessToken`; exchange it via
`POST /api/auth/refresh` and revoke it via `POST /api/auth/logout`. Documented in [§4.1](#41-auth).
Single-use (rotated on every exchange) and server-revocable (Redis-backed), unlike the access token.

### 6.4 Still open, workable as-is

- **No batch user lookup.** See note 1 in §5 — cache a user map client-side.
- **No OpenAPI/Swagger.** `springdoc-openapi` is not a dependency; this document is the spec. If you
  want generated TypeScript types, adding springdoc would give you a schema to generate from.
- **No `GET /api/users/active` or free-text user search** — filter `GET /api/users` on
  `name`/`surname` and drop inactive rows client-side.
- **No default sort** on list endpoints; pass `sort=` explicitly.
