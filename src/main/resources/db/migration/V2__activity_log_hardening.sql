-- ============================================================
-- V2__activity_log_hardening.sql
-- Internal Issue Tracker - Activity log made fit to compute metrics from
-- ============================================================
--
-- V1 designed the three activity tables but nothing ever wrote to them. Before
-- the `activity` module starts, three things have to be true that are not:
--
--   1. `created_at` must be mandatory. Every agile metric is a difference
--      between two activity rows, so a null timestamp is not a missing field -
--      it is an issue that silently drops out of cycle time.
--
--   2. `issue_activities` must carry `project_id`. Every metric is scoped to a
--      project, and without the column each query would have to join `issues` -
--      an `activity` module reading an `issue` module table. That is the exact
--      violation `shared/package-info.java` warns ModularityTests cannot see,
--      because a query is a string.
--
--   3. The indexes must match how the metrics read. V1 indexed each foreign key
--      on its own; every metric query filters by parent AND orders by time.
--
-- A note on the backfill below: the rows it writes are indistinguishable from
-- rows the application wrote, because there is no column to tell them apart.
-- That is accepted rather than worked around - both columns it derives from
-- (`issues.created_at`, `issues.reporter_id`) are authoritative, so the rows are
-- not guesses. Nothing else is backfilled; see the comment on that statement.

-- ============================================================
-- 1. created_at IS THE MEASUREMENT, SO IT IS MANDATORY
-- ============================================================

-- Defensive: the tables have never had a writer, so this should match nothing.
-- It exists so the ALTERs below cannot fail on an environment where a row was
-- inserted by hand with an explicit NULL.
UPDATE "issue_activities"   SET "created_at" = now() WHERE "created_at" IS NULL;
UPDATE "sprint_activities"  SET "created_at" = now() WHERE "created_at" IS NULL;
UPDATE "project_activities" SET "created_at" = now() WHERE "created_at" IS NULL;

ALTER TABLE "issue_activities"   ALTER COLUMN "created_at" SET NOT NULL;
ALTER TABLE "sprint_activities"  ALTER COLUMN "created_at" SET NOT NULL;
ALTER TABLE "project_activities" ALTER COLUMN "created_at" SET NOT NULL;

-- DEFAULT (now()) is deliberately left in place. The application always writes
-- the column explicitly - it carries the moment the change happened, not the
-- moment the row was inserted, which are different once the listener is
-- asynchronous. The default is only a net under a hand-written INSERT.

-- ============================================================
-- 2. project_id ON issue_activities
-- ============================================================

ALTER TABLE "issue_activities" ADD COLUMN "project_id" int;

UPDATE "issue_activities" a
   SET "project_id" = i."project_id"
  FROM "issues" i
 WHERE i."id" = a."issue_id";

ALTER TABLE "issue_activities" ALTER COLUMN "project_id" SET NOT NULL;
ALTER TABLE "issue_activities" ADD FOREIGN KEY ("project_id") REFERENCES "projects" ("id");

-- Denormalised, and safe to denormalise: an issue never moves between projects.
-- `issues.project_id` is NOT NULL and no service writes it after creation, so
-- this copy cannot go stale. If issues ever become movable, this column has to
-- move with them - and the activity row should keep the project it was filed
-- under anyway, which is what makes the copy the right shape rather than a join.

-- ============================================================
-- 3. BACKFILL - CREATED ONLY
-- ============================================================

INSERT INTO "issue_activities" ("issue_id", "project_id", "user_id", "action_type", "created_at")
SELECT i."id", i."project_id", i."reporter_id", 'CREATED', COALESCE(i."created_at", now())
  FROM "issues" i
 WHERE NOT EXISTS (SELECT 1
                     FROM "issue_activities" a
                    WHERE a."issue_id" = i."id"
                      AND a."action_type" = 'CREATED');

-- Lead time is measured from the CREATED row. Without this, every issue that
-- already exists would be missing its starting point and would drop out of the
-- metric - not visibly, but by simply not being counted, so the numbers would
-- look right while describing only issues filed after this migration.
--
-- Nothing else is backfilled, and the reason is the same in each case: the row
-- would have to invent its actor. `issues.deleted_at` gives the moment of a
-- DELETED row but not who did it, and whoever deleted an issue is generally not
-- its reporter. `issues.status` gives the current status but neither the moment
-- it was reached nor who moved it. An audit row with a wrong actor is worse than
-- an absent one, so those histories start empty and fill from here on.

-- ============================================================
-- 4. INDEXES SHAPED LIKE THE METRIC QUERIES
-- ============================================================
-- Every metric filters on a parent and then walks time: "this project's status
-- changes, in order" or "this issue's timeline". The leading column narrows, the
-- trailing created_at serves both the range predicate and the ordering that
-- lead() windows need.

CREATE INDEX ON "issue_activities" ("issue_id", "created_at");
CREATE INDEX ON "issue_activities" ("project_id", "created_at");
CREATE INDEX ON "issue_activities" ("user_id", "created_at");

-- Throughput, reopen rate and cycle time all filter action_type before ordering.
CREATE INDEX ON "issue_activities" ("project_id", "action_type", "created_at");

CREATE INDEX ON "sprint_activities" ("sprint_id", "created_at");
CREATE INDEX ON "sprint_activities" ("user_id", "created_at");

CREATE INDEX ON "project_activities" ("project_id", "created_at");
CREATE INDEX ON "project_activities" ("user_id", "created_at");

-- ============================================================
-- 5. DROP THE SINGLE-COLUMN INDEXES THE COMPOSITES SUBSUME
-- ============================================================
-- Each of these is now a prefix of a composite above, so it answers nothing the
-- composite does not while still costing a write on every insert. Names are
-- PostgreSQL's own, from V1's unnamed CREATE INDEX statements.

DROP INDEX IF EXISTS "issue_activities_issue_id_idx";
DROP INDEX IF EXISTS "issue_activities_user_id_idx";
DROP INDEX IF EXISTS "sprint_activities_sprint_id_idx";
DROP INDEX IF EXISTS "sprint_activities_user_id_idx";
DROP INDEX IF EXISTS "project_activities_project_id_idx";
DROP INDEX IF EXISTS "project_activities_user_id_idx";
