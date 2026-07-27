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

## Project Status

🚧 **Early development — foundational infrastructure and the first module (`user`) are underway.**

What's currently in place:

- [x] Project scaffolding (Spring Boot + Maven)
- [x] Docker Compose setup for local PostgreSQL
- [x] Database schema designed (dbdiagram.io) and implemented via Flyway migrations
- [x] Core dependencies wired up (Spring Modulith, Spring Data JPA, Spring Security, Validation)
- [x] Module boundaries scaffolded (`user`, `team`, `project`, `sprint`, `epic`, `issue`, `comment`, `activity`,
  `shared`)
- [x] Shared exception hierarchy and global exception handler
- [x] Shared API response wrapper with pagination support
- [x] `user` module: `User` entity and `UserRepository`
- [x] `user` module: service, controller and DTOs (CRUD + password management)
- [x] JWT authentication (`POST /api/auth/login`), stateless security filter chain

### Roadmap

- [ ] Finish the `user` module (search & filtering endpoints, `GET /api/auth/me`)
- [x] Authentication (Spring Security + JWT bearer tokens)
- [ ] Implement remaining domain entities and repositories (team, project, sprint, epic, issue, comment)
- [ ] Core CRUD APIs for projects, sprints, epics, and issues
- [ ] Role-based authorization (`ADMIN` / `DEVELOPER` / `USER`) plus project-scoped access
  — see [Roles & Permissions](#roles--permissions-planned)
- [ ] Activity/audit logging via Spring Modulith events
- [ ] API documentation
- [ ] Test coverage (unit, module, integration)

## Roles & Permissions (Planned)

Authorization is built on **two independent axes**. Keeping them separate matters: a global role answers *"is this
kind of user allowed to do this kind of thing?"*, while project membership answers *"is this user allowed to do it
**here**?"*. Both checks apply.

### 1. Global role (`users.role`)

A single enum column on the user, replacing the current `is_admin` boolean.

| Role        | Intent                | Capabilities                                                                          |
|-------------|-----------------------|---------------------------------------------------------------------------------------|
| `ADMIN`     | System administrator  | Full access to every endpoint; manages users, roles, teams and projects                |
| `DEVELOPER` | Regular contributor   | Joins teams, creates issues, takes/receives assignments, comments, moves issues        |
| `USER`      | Read-only stakeholder | Views only; added to a project solely by that project's leader to follow its progress  |

Roles are hierarchical — `ADMIN` implies `DEVELOPER` implies `USER` — so a rule written as `hasRole('USER')` also
admits developers and admins, and each endpoint only needs to declare its *minimum* role.

### 2. Project-scoped access (`projects.leader_id`, `project_users`, `project_teams`)

"Project leader" is deliberately **not** a global role: it is a relationship stored as data. The same person can lead
one project while being an ordinary member of another. Fine-grained checks ("is the caller a member of this project?",
"is the caller its leader?") resolve against these tables, exposed to the security layer through a
`ProjectMembership` port in `shared.security` — the same pattern already used by `AuthenticatedUserLookup`.

### Design notes

- The role is **not** carried as a JWT claim. It is resolved from the database on every request (see
  `UserAuthenticatedUserLookup`), so a role change or deactivation takes effect immediately rather than at token
  expiry.
- Authorization rules stay centralized in `SecurityConfig` rather than scattered across `@PreAuthorize` annotations.
- Self-registration always creates a `USER`; the create/update DTOs never accept a role field. Promotion is an
  admin-only operation.
- Role changes are guarded against lockout: an admin cannot change their own role, and the last active admin cannot
  be demoted.

> **Status:** designed, not yet implemented — scheduled after the `project` and `sprint` modules land, so the
> project-scoped half of the model can be built against real entities.

## User Endpoints (Planned)

### Core CRUD

- [x] `POST /api/users/register` — Register a new user (public; always created with the `USER` role)
- [x] `GET /api/users/{id}` — Get user details by id
- [x] `GET /api/users` — List users (paginated, filterable)
- [x] `PUT /api/users/{id}` — Update user details (name/surname/email)
- [x] `DELETE /api/users/{id}` — Deactivate a user (soft delete, `is_active=false`)

### Password Management

- [x] `PATCH /api/users/{id}/password` — Change password (requires current password confirmation)
- [x] `POST /api/users/{id}/reset-password` — Admin-initiated password reset (future phase)

### Role / Permission

- [ ] `PATCH /api/users/{id}/role` — Change a user's global role (Admin only; cannot target self, cannot demote the
  last active admin)

Until this endpoint exists, roles are set directly in the database by whoever has DB access.

### Authentication (once the security layer is in place)

- [x] `POST /api/auth/login` — Log in, returns a JWT token
- [ ] `POST /api/auth/refresh` — Refresh token (future phase)
- [ ] `GET /api/auth/me` — Get the currently authenticated user's details

### Filtering / Utility

- [ ] `GET /api/users/active` — List only active users
- [ ] `GET /api/users/search?q=...` — Search by name/surname/email (future phase)

---
This README will be updated as the project progresses.
---

## License

This project is licensed under the [MIT License](./LICENSE).
