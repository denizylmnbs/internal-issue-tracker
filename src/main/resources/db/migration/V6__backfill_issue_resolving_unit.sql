-- ============================================================
-- V6__backfill_issue_resolving_unit.sql
-- Internal Issue Tracker - backfill resolving_unit from assignee_team_id
-- ============================================================
--
-- V5 only added the column; issues filed before it existed were left null.
-- This backfills from the assigned team's field where it names one of the
-- four units. Issues with no team, or a team fielded DESIGN/DATA/unset, stay
-- null - there is nothing here to derive a unit from.

UPDATE "issues" i
   SET "resolving_unit" = t."field"
  FROM "teams" t
 WHERE t."id" = i."assignee_team_id"
   AND t."field" IN ('BACKEND', 'FRONTEND', 'IOS', 'ANDROID');
