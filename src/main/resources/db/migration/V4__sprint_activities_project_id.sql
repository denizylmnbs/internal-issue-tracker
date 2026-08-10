-- ============================================================
-- V4__sprint_activities_project_id.sql
-- Internal Issue Tracker - sprint_activities gets a project_id too
-- ============================================================
--
-- GET /api/projects/{id}/activities now unions all three activity tables into
-- one project-wide feed instead of returning project_activities alone - a
-- status change on an issue or the creation of a sprint previously had no way
-- to show up there at all. issue_activities already carries project_id
-- (V2__activity_log_hardening.sql); this gives sprint_activities the same
-- column for the same reason: without it, unioning would mean this module
-- joining sprint's own `sprints` table, which is exactly the cross-module
-- read `shared/package-info.java` warns ModularityTests cannot catch,
-- because a query is a string.

ALTER TABLE "sprint_activities" ADD COLUMN "project_id" int;

UPDATE "sprint_activities" a
   SET "project_id" = s."project_id"
  FROM "sprints" s
 WHERE s."id" = a."sprint_id";

ALTER TABLE "sprint_activities" ALTER COLUMN "project_id" SET NOT NULL;
ALTER TABLE "sprint_activities" ADD FOREIGN KEY ("project_id") REFERENCES "projects" ("id");

-- Denormalised, and safe to denormalise for the same reason as
-- issue_activities.project_id: a sprint never moves between projects.

CREATE INDEX ON "sprint_activities" ("project_id", "created_at");
