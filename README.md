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

Internal Issue Tracker is a project/sprint/issue tracking application intended for internal team use, similar in spirit to Jira. It manages users, teams, projects, sprints, epics, issues, comments, and full activity history for auditing purposes.

The project is deliberately built as a **Modular Monolith** rather than microservices: a single deployable application internally organized into independent, well-bounded modules (via [Spring Modulith](https://spring.io/projects/spring-modulith)), each owning its own domain logic and enforcing boundaries at compile/verification time. This gives the simplicity of a monolith (one deployment, one database, easy local development) while keeping the codebase modular enough to split into separate services later if needed.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Architecture | Spring Modulith 2.1.0 (Modular Monolith) |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 17 |
| Migrations | Flyway |
| Security | Spring Security |
| Validation | Spring Validation (Jakarta Bean Validation) |
| Build Tool | Maven (via Maven Wrapper) |
| Boilerplate Reduction | Lombok |
| Local Infrastructure | Docker Compose |

## Database Schema

The database schema was designed with [dbdiagram.io](https://dbdiagram.io) and is version-controlled through Flyway migrations under [`src/main/resources/db/migration`](./src/main/resources/db/migration).

<!-- TODO: Replace with the exported dbdiagram.io schema image, e.g.: -->
<!-- ![Database Schema](./docs/db-schema.png) -->

Core entities include: `users`, `teams`, `team_users`, `projects`, `project_teams`, `project_users`, `sprints`, `epics`, `issues`, `comments`, and per-entity activity logs (`issue_activities`, `sprint_activities`, `project_activities`) for tracking changes over time.

## Project Structure

The codebase follows the conventions of a Spring Modulith application, where each business capability lives in its own top-level package (module) under the base package, with internal classes kept package-private and only intended APIs exposed publicly.

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

Each module is expected to be verified for boundary violations using Spring Modulith's testing support (`ApplicationModules.verify()`) as the codebase grows.

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

On startup, Flyway automatically applies all pending migrations under `src/main/resources/db/migration` against the configured database.

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
- [x] Module boundaries scaffolded (`user`, `team`, `project`, `sprint`, `epic`, `issue`, `comment`, `activity`, `shared`)
- [x] Shared exception hierarchy and global exception handler
- [x] Shared API response wrapper with pagination support
- [x] `user` module: `User` entity and `UserRepository`

### Roadmap

- [ ] Complete `user` module (service, controller, DTOs)
- [ ] Implement remaining domain entities and repositories (team, project, sprint, epic, issue, comment)
- [ ] Authentication & authorization (Spring Security)
- [ ] Core CRUD APIs for projects, sprints, epics, and issues
- [ ] Activity/audit logging via Spring Modulith events
- [ ] API documentation
- [ ] Test coverage (unit, module, integration)

## User Endpoints (Planned)

### Core CRUD
- [ ] `POST /api/users` — Create a new user (requires Admin role)
- [ ] `GET /api/users/{id}` — Get user details by id
- [ ] `GET /api/users` — List users (paginated, filterable)
- [ ] `PUT /api/users/{id}` — Update user details (name/surname/email)
- [ ] `DELETE /api/users/{id}` — Deactivate a user (soft delete, `is_active=false`)

### Password Management
- [ ] `PATCH /api/users/{id}/password` — Change password (requires current password confirmation)
- [ ] `POST /api/users/{id}/reset-password` — Admin-initiated password reset (future phase)

### Role / Permission
- [ ] `PATCH /api/users/{id}/admin-status` — Grant or revoke admin privileges (Admin only)

### Authentication (once the security layer is in place)
- [ ] `POST /api/auth/login` — Log in, returns a JWT token
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
