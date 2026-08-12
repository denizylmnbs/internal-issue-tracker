-- ============================================================
-- V9__drop_enum_checks.sql
-- Internal Issue Tracker - hand the classification vocabularies to field_definitions
-- ============================================================
--
-- The CHECK constraints below were the second, independently-maintained
-- copy of each enum's value set (see V1, V5). Now that field_definitions
-- (V7) is the single source of truth - enforced by application-level
-- validation against FieldDefinitionLookup, since the set of valid codes is
-- per-project data a CHECK constraint cannot see - these constraints would
-- only ever reject a value the application had already accepted, or block
-- a project from defining one of its own. Run only once V7/V8 have been
-- applied and the application is deployed on the new validation path.
--
-- Columns widen to varchar(30) to match field_definitions.code; nothing
-- stored today exceeds 20 characters, so this is a widening, not a
-- narrowing - migration-free in the direction V5's own comment cared about.

ALTER TABLE "issues" DROP CONSTRAINT IF EXISTS issues_status_check;
ALTER TABLE "issues" DROP CONSTRAINT IF EXISTS issues_type_check;
ALTER TABLE "issues" DROP CONSTRAINT IF EXISTS issues_priority_check;
ALTER TABLE "issues" DROP CONSTRAINT IF EXISTS issues_resolving_unit_check;
ALTER TABLE "issues" ALTER COLUMN "status" TYPE varchar(30);
ALTER TABLE "issues" ALTER COLUMN "type" TYPE varchar(30);
ALTER TABLE "issues" ALTER COLUMN "priority" TYPE varchar(30);
ALTER TABLE "issues" ALTER COLUMN "resolving_unit" TYPE varchar(30);

ALTER TABLE "sprints" DROP CONSTRAINT IF EXISTS sprints_status_check;
ALTER TABLE "sprints" ALTER COLUMN "status" TYPE varchar(30);

ALTER TABLE "epics" DROP CONSTRAINT IF EXISTS epics_status_check;
ALTER TABLE "epics" ALTER COLUMN "status" TYPE varchar(30);

ALTER TABLE "projects" DROP CONSTRAINT IF EXISTS projects_status_check;
ALTER TABLE "projects" ALTER COLUMN "status" TYPE varchar(30);

ALTER TABLE "teams" DROP CONSTRAINT IF EXISTS teams_field_check;

-- issue_activities.issue_type / .priority (V3): same reasoning, these are
-- read by the metric queries as bound parameters now, not literals, so a
-- value need not satisfy a CHECK to be matched correctly.
ALTER TABLE "issue_activities" DROP CONSTRAINT IF EXISTS issue_activities_issue_type_check;
ALTER TABLE "issue_activities" DROP CONSTRAINT IF EXISTS issue_activities_priority_check;
ALTER TABLE "issue_activities" ALTER COLUMN "issue_type" TYPE varchar(30);
ALTER TABLE "issue_activities" ALTER COLUMN "priority" TYPE varchar(30);

-- issue_activities.action_type is NOT touched - IssueActionType is a fixed
-- storage vocabulary bound to a database constraint on purpose (see
-- IssueActionType's own javadoc) and stays out of this migration's scope,
-- as do the equivalent columns on sprint_activities / project_activities.

-- ============================================================
-- one_active_sprint_per_project replacement
-- ============================================================
--
-- The old partial unique index matched the literal status name
-- 'IN_PROGRESS'. That name is no longer guaranteed to exist - a project may
-- rename or replace it - so the "one running sprint per project" guarantee
-- moves onto a column SprintService derives from the is_active_work flag of
-- whatever status a sprint is given, rather than from the status text
-- itself.

DROP INDEX IF EXISTS one_active_sprint_per_project;

ALTER TABLE "sprints" ADD COLUMN "is_running" boolean NOT NULL DEFAULT false;

-- Backfill from the status text one last time, while it is still known to
-- mean IN_PROGRESS for every project (V8 seeded every project with the same
-- template). From here on SprintService writes this column directly.
UPDATE "sprints" SET "is_running" = true WHERE "status" = 'IN_PROGRESS';

CREATE UNIQUE INDEX one_active_sprint_per_project
    ON "sprints" ("project_id") WHERE "is_running" AND "deleted_at" IS NULL;
