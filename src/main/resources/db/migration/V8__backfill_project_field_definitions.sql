-- ============================================================
-- V8__backfill_project_field_definitions.sql
-- Internal Issue Tracker - give every existing project its own copy
-- ============================================================
--
-- V7 only seeded the global template rows (project_id IS NULL). Every
-- project that already exists needs its own rows for the six project-scoped
-- kinds, exactly the way FieldDefinitionProvisioning#seedDefaults gives one
-- to every project created from now on - a project's issues/sprints/epics
-- are validated and defaulted against its own rows, never the template's.

INSERT INTO "field_definitions"
    ("kind", "project_id", "code", "label", "color", "sort_order",
     "is_default", "is_done", "is_cancelled", "is_active_work", "is_defect")
SELECT t."kind", p."id", t."code", t."label", t."color", t."sort_order",
       t."is_default", t."is_done", t."is_cancelled", t."is_active_work", t."is_defect"
  FROM "projects" p
 CROSS JOIN "field_definitions" t
 WHERE t."project_id" IS NULL
   AND t."kind" IN ('SPRINT_STATUS', 'EPIC_STATUS', 'ISSUE_STATUS',
                    'ISSUE_TYPE', 'ISSUE_PRIORITY', 'ISSUE_UNIT');
