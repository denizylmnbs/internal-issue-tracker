# Internal Issue Tracker

> A Jira-style internal issue tracking system built as a **Modular Monolith** with Spring Boot and Spring Modulith.

<p align="left">
  <img src="https://img.shields.io/badge/status-in%20development-yellow?style=flat-square" alt="Project Status" />
  <img src="https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java 25" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 4.1.0" />
  <img src="https://img.shields.io/badge/Spring%20Modulith-2.1.0-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Modulith 2.1.0" />
  <img src="https://img.shields.io/badge/PostgreSQL-17-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL 17" />
  <img src="https://img.shields.io/badge/Flyway-Migrations-CC0200?style=flat-square&logo=flyway&logoColor=white" alt="Flyway" />
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven" />
  <img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="MIT License" />
</p>

## About

Internal Issue Tracker is a project/sprint/issue tracking application intended for internal team use, similar in spirit
to Jira. It manages users, teams, projects, sprints, epics, issues, comments, and full activity history for auditing
purposes.

The project is deliberately built as a **Modular Monolith** rather than microservices: a single deployable application
internally organized into independent, well-bounded modules (
via [Spring Modulith](https://spring.io/projects/spring-modulith)), each owning its own domain logic and enforcing
boundaries at compile/verification time. This gives the simplicity of a monolith (one deployment, one database, easy
local development) while keeping the codebase modular enough to split into separate services later if needed.

## Tech Stack

| Layer                 | Technology                                  |
|-----------------------|---------------------------------------------|
| Language              | Java 25                                     |
| Framework             | Spring Boot 4.1.0                           |
| Architecture          | Spring Modulith 2.1.0 (Modular Monolith)    |
| Web                   | Spring Web MVC                              |
| Persistence           | Spring Data JPA + Hibernate                 |
| Database              | PostgreSQL 17                               |
| Migrations            | Flyway                                      |
| Security              | Spring Security                             |
| Validation            | Spring Validation (Jakarta Bean Validation) |
| Build Tool            | Maven (via Maven Wrapper)                   |
| Boilerplate Reduction | Lombok                                      |
| Local Infrastructure  | Docker Compose                              |

## Database Schema

The database schema was designed with [dbdiagram.io](https://dbdiagram.io) and is version-controlled through Flyway
migrations under [`src/main/resources/db/migration`](./src/main/resources/db/migration).

<!-- TODO: Replace with the exported dbdiagram.io schema image, e.g.: -->
<!-- ![Database Schema](./docs/db-schema.png) -->

Core entities include: `users`, `teams`, `team_users`, `projects`, `project_teams`, `project_users`, `sprints`, `epics`,
`issues`, `comments`, and per-entity activity logs (`issue_activities`, `sprint_activities`, `project_activities`) for
tracking changes over time.

## Project Structure

The codebase follows the conventions of a Spring Modulith application, where each business capability lives in its own
top-level package (module) under the base package, with internal classes kept package-private and only intended APIs
exposed publicly.

```
internal-issue-tracker/
├── src/
│   ├── main/
│   │   ├── java/com/ist/internal_issue_tracker/
│   │   │   ├── InternalIssueTrackerApplication.java   # Application entry point
│   │   │   └── ...                                    # Feature modules (user, team, project, sprint, issue, ...)
│   │   └── resources/
│   │       ├── application.properties                 # Application configuration
│   │       ├── db/migration/                           # Flyway SQL migrations (versioned schema)
│   │       ├── static/                                 # Static web assets
│   │       └── templates/                               # Server-side templates
│   └── test/
│       └── java/com/ist/internal_issue_tracker/        # Tests (unit, module, integration)
├── compose.yaml                                         # Local PostgreSQL via Docker Compose
├── pom.xml                                               # Maven project & dependency definitions
├── .env.example                                          # Example environment variables
└── mvnw / mvnw.cmd                                       # Maven Wrapper
```

Each module is expected to be verified for boundary violations using Spring Modulith's testing support (
`ApplicationModules.verify()`) as the codebase grows.

## Getting Started

### Prerequisites

- Java 25 (JDK)
- Docker & Docker Compose (for local PostgreSQL)
- Maven Wrapper is included, so a local Maven install is not required

### 1. Clone the repository

```bash
git clone https://github.com/denizylmnbs/internal-issue-tracker.git
cd internal-issue-tracker
```

### 2. Configure environment variables

Copy the example environment file and adjust the values as needed:

```bash
cp .env.example .env
```

```env
POSTGRES_DB=issue_tracker
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change_me
POSTGRES_PORT=5432
```

### 3. Start the database

```bash
docker compose up -d
```

This starts a PostgreSQL 17 container (`issue-tracker-db`) and applies persistent storage via a named Docker volume.

### 4. Run the application

```bash
./mvnw spring-boot:run
```

On startup, Flyway automatically applies all pending migrations under `src/main/resources/db/migration` against the
configured database.

### 5. Run the tests

```bash
./mvnw test
```

## API Documentation

**→ [`docs/API.md`](./docs/API.md)** — the complete reference for all 79 endpoints: request and
response shapes, query parameters, authorization rules per route, every error code, and the
behavioural rules a client has to know (full-replacement `PUT`s, sparse metric series, the
`/members` vs `/participants` distinction, and so on).

## Project Status

🚀 **Backend feature-complete.** Every domain module is implemented, wired through the event-driven
activity log, and covered by the authorization model below.

What's in place:

- [x] Project scaffolding (Spring Boot + Maven) and Docker Compose PostgreSQL
- [x] Database schema via Flyway migrations (`V1` init, `V2` activity hardening, `V3` metric dimensions)
- [x] Module boundaries enforced at build time (`ModularityTests`)
- [x] Shared exception hierarchy, global exception handler, single response envelope, paged responses
- [x] JWT authentication (`POST /api/auth/login`), stateless security filter chain
- [x] Role hierarchy (`ADMIN → EDITOR → DEVELOPER → USER`) plus project-scoped authorization
- [x] `user` module — CRUD, password management, role changes with lockout guards
- [x] `team` module — teams, membership, leadership
- [x] `project` module — projects, direct members, assigned teams, participants, leadership
- [x] `sprint` module — sprints with one-running-sprint enforcement and commitment snapshots
- [x] `epic` module — epics and their lifecycle
- [x] `issue` module — issues, status, dual (user + team) assignment, filtering
- [x] `comment` module — comments with author-scoped editing
- [x] `activity` module — issue / sprint / project audit logs via Spring Modulith events
- [x] `activity.metrics` — 14 agile metrics computed from the log
- [x] API documentation ([`docs/API.md`](./docs/API.md))
- [x] CORS for a browser frontend (`CORS_ALLOWED_ORIGINS`, defaults to `http://localhost:3000`)
- [x] `GET /api/auth/me` — the authenticated caller's own record, role included

### Roadmap

- [ ] Next.js frontend
- [ ] `POST /api/auth/refresh` — refresh tokens; sessions currently end abruptly at expiry
- [ ] Batch user lookup, so clients stop resolving ids one at a time
- [ ] OpenAPI/Swagger (`springdoc-openapi`), for generated client types
- [ ] Broader test coverage — the suite runs without a database today, so anything touching
  persistence (including `contextLoads`) is verified by hand

## Roles & Permissions

Authorization is built on **two independent axes**. Keeping them separate matters: a global role answers *"is this
kind of user allowed to do this kind of thing?"*, while project membership answers *"is this user allowed to do it
**here**?"*. Both checks apply.

### 1. Global role (`users.role`)

A single enum column on the user, replacing the current `is_admin` boolean.

| Role        | Intent                | Capabilities                                                                          |
|-------------|-----------------------|---------------------------------------------------------------------------------------|
| `ADMIN`     | System administrator  | Full access to every endpoint; manages users and roles                                 |
| `EDITOR`    | Delivery manager      | Creates and deletes teams and projects; may act on any project regardless of leadership |
| `DEVELOPER` | Regular contributor   | Joins teams, creates issues, takes/receives assignments, comments, moves issues        |
| `USER`      | Read-only stakeholder | Views only; cannot be added to a team or a project at all                              |

Roles are hierarchical — `ADMIN` implies `EDITOR` implies `DEVELOPER` implies `USER` — so a rule written as
`hasRole('DEVELOPER')` also admits editors and admins, and each endpoint only needs to declare its *minimum* role.

Note that `DEVELOPER` is the **minimum role for membership**: adding a plain `USER` to a team or a project is
rejected with `403 USER_ROLE_NOT_ENOUGH`.

### 2. Project-scoped access (`projects.leader_id`, `project_users`, `project_teams`)

"Project leader" is deliberately **not** a global role: it is a relationship stored as data. The same person can lead
one project while being an ordinary member of another. Fine-grained checks ("is the caller a participant of this
project?", "is the caller its leader?") resolve against these tables, exposed to the security layer through the
`ProjectLookup` and `TeamLookup` ports in `shared.port` — the same pattern already used by `AuthenticatedUserLookup`.

Three rules are built on top of those ports in `SecurityConfig`:

| Rule | Admits |
|------|--------|
| `editorOrTeamLeader` | `EDITOR`+, or the leader of *this* team |
| `editorOrProjectLeader` | `EDITOR`+, or the leader of *this* project — planning artifacts (sprints, epics) and destructive issue operations |
| `editorLeaderOrParticipant` | the above, plus anyone actually working on the project — a direct member **or** a member of an assigned team |

Participation is what the issue and comment routes run on: refusing a developer the right to file or move their own
work would make the tracker unusable. It also gates the activity feeds and the metrics — the only reads restricted
beyond being logged in, since a team that cannot see its own flow cannot improve it.

### Design notes

- The role is **not** carried as a JWT claim. It is resolved from the database on every request (see
  `UserAuthenticatedUserLookup`), so a role change or deactivation takes effect immediately rather than at token
  expiry.
- Authorization rules stay centralized in `SecurityConfig` rather than scattered across `@PreAuthorize` annotations.
- Self-registration always creates a `USER`; the create/update DTOs never accept a role field. Promotion is an
  admin-only operation.
- Role changes are guarded against lockout by a single rule: the caller must **strictly outrank both** the target's
  current role and the requested new role. An admin therefore cannot change their own role, cannot demote another
  admin, and cannot promote anyone to admin.
- Nested routes name the project path variable `id` rather than `projectId`, because the authorization managers read
  it literally out of `RequestAuthorizationContext#getVariables()`. That naming is what lets one rule cover
  `/api/projects/{id}/issues/{issueId}/comments/{commentId}` without a port of its own.

## Endpoints

All 79 endpoints are documented in **[`docs/API.md`](./docs/API.md)**. Summary:

| Area | Base path | Count |
|------|-----------|-------|
| Auth | `/api/auth` | 2 |
| Users | `/api/users` | 10 |
| Teams & membership | `/api/teams` | 10 |
| Projects | `/api/projects` | 8 |
| Project members | `/api/projects/{id}/members`, `/participants` | 4 |
| Project teams | `/api/projects/{id}/teams` | 3 |
| Sprints | `/api/projects/{id}/sprints` | 6 |
| Epics | `/api/projects/{id}/epics` | 6 |
| Issues | `/api/projects/{id}/issues` | 8 |
| Comments | `/api/projects/{id}/issues/{issueId}/comments` | 5 |
| Activity log | `/api/projects/{id}/**/activities` | 3 |
| Metrics | `/api/projects/{id}/metrics` | 14 |

## Metrics

The `activity.metrics` module computes 14 agile metrics from the activity log rather than from the issues' current
state, which is what makes them historical rather than a snapshot: **cycle time**, **lead time**, **bug MTTR**,
**throughput** (and a breakdown by type or priority), **time in status**, **flow efficiency**, **reopen rate**,
**net flow**, **defect ratio**, **WIP** with an aging list, **velocity**, **burndown**, and a **cumulative flow
diagram**.

They are project-level aggregates with no per-person breakdown, deliberately: a metric that becomes an individual
performance measure stops describing the work and starts describing how people respond to being measured — which
would corrupt the log it is all computed from.

## License

This project is licensed under the [MIT License](./LICENSE).
