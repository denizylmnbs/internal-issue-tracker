-- ============================================================
-- V5__issue_resolving_unit.sql
-- Internal Issue Tracker - which unit resolves an issue
-- ============================================================
--
-- Optional, like "type": nullable until triage assigns it, and widening the
-- CHECK later stays migration-free while narrowing it would not.

ALTER TABLE "issues" ADD COLUMN "resolving_unit" varchar(20)
    CHECK ("resolving_unit" IN ('BACKEND', 'FRONTEND', 'IOS', 'ANDROID'));
