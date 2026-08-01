-- ============================================================
-- V3__metric_dimensions.sql
-- Internal Issue Tracker - The dimensions the metrics slice by
-- ============================================================
--
-- V2 made the activity log measurable in time. This makes it measurable by
-- something other than time.
--
-- Every metric added on top of the first six - throughput by type, bug ratio,
-- defect density, bug MTTR, velocity, burndown - is the same count or duration
-- cut by an attribute of the issue: its type, its priority, its estimate, the
-- sprint it was in. None of those live in `issue_activities`, and the module
-- that owns that table may not read `issues` to find them. That is the same
-- boundary V2's `project_id` column answered, so this answers it the same way.
--
-- WHY AS-OF VALUES RATHER THAN A JOIN
--
-- These four columns are not a cache of the issue's current state. They hold
-- what was true at the moment of the activity, which is what a fact table is
-- for. An issue re-pointed from 3 to 8 today must not silently rewrite the
-- velocity of the sprint it was delivered in three months ago, and an issue
-- moved from one sprint to the next must not disappear from the first sprint's
-- burndown as though it had never been there. A join to `issues` would do both,
-- quietly, and the numbers would keep looking plausible.
--
-- So `project_id` is denormalised because it cannot change, and these are
-- denormalised because they can.
--
-- The backfill below is the one place that rule is broken, and it is broken
-- knowingly - see the comment on it.

-- ============================================================
-- 1. THE FOUR DIMENSION COLUMNS
-- ============================================================
-- All nullable, unlike `project_id`. An issue may genuinely have no type, no
-- estimate and no sprint, so null here means "not set", not "not recorded".

ALTER TABLE "issue_activities" ADD COLUMN "issue_type" varchar(20)
    CHECK ("issue_type" IN ('BUG','FEATURE','STORY','TASK','ENHANCEMENT','REFACTOR'));

ALTER TABLE "issue_activities" ADD COLUMN "priority" varchar(20)
    CHECK ("priority" IN ('LOW','MEDIUM','HIGH','CRITICAL'));

ALTER TABLE "issue_activities" ADD COLUMN "story_point" int;

ALTER TABLE "issue_activities" ADD COLUMN "sprint_id" int;

ALTER TABLE "issue_activities" ADD FOREIGN KEY ("sprint_id") REFERENCES "sprints" ("id");

-- The CHECK constraints mirror `issues`. They are worth having even though the
-- application is the only writer: these columns are read by string literal in
-- the metric queries, so a value that is merely misspelled would not fail, it
-- would just stop matching and shrink a count without saying so.

-- ============================================================
-- 2. BACKFILL - CURRENT STATE, KNOWINGLY APPROXIMATE
-- ============================================================

UPDATE "issue_activities" a
   SET "issue_type"  = i."type",
       "priority"    = i."priority",
       "story_point" = i."story_point",
       "sprint_id"   = i."sprint_id"
  FROM "issues" i
 WHERE i."id" = a."issue_id";

-- This writes today's values onto yesterday's rows, which is exactly what the
-- header says these columns must not hold. It is accepted here for the same
-- reason V2 backfilled CREATED and nothing else: the alternative is worse.
--
-- Leaving them null would drop every pre-existing issue out of every dimensioned
-- metric - not visibly, but by not being counted, so a bug ratio would report on
-- issues filed after this migration while looking like it reported on all of
-- them. Writing the current value is wrong only where the attribute has since
-- changed, and it is right everywhere else.
--
-- The history that would let this be exact does not exist: PRIORITY_UPDATED and
-- STORY_POINT_UPDATED rows carry both sides of each change and could be replayed
-- backwards, but the log only started at V2, so there is nothing to replay. From
-- here on the application writes the as-of value on every row and this
-- approximation stops spreading.

-- ============================================================
-- 3. INDEXES FOR THE DIMENSIONED READS
-- ============================================================

-- Velocity and burndown both start from "everything that happened in this
-- sprint, in order". Partial, because most rows on most projects carry no
-- sprint and there is no reason to index the nulls.
CREATE INDEX ON "issue_activities" ("sprint_id", "created_at")
    WHERE "sprint_id" IS NOT NULL;

-- Bug ratio, defect density and bug MTTR all narrow to one type first.
CREATE INDEX ON "issue_activities" ("project_id", "issue_type", "action_type", "created_at");

-- ============================================================
-- 4. SPRINT COMMITMENT
-- ============================================================

ALTER TABLE "sprints" ADD COLUMN "committed_points" int;
ALTER TABLE "sprints" ADD COLUMN "committed_at" timestamptz;

-- Velocity is two numbers, not one: what the team said it would deliver and what
-- it delivered. Only the second is derivable from the activity log, and a
-- velocity reported without the first is just throughput measured in points -
-- it cannot say whether the team is over-committing, which is the question
-- velocity exists to answer.
--
-- `committed_points` is written once, by the application, at the moment the
-- sprint first moves to IN_PROGRESS, as the sum of the story points then in it.
-- Automatic rather than typed in, because a number someone has to remember to
-- enter is a number that is missing on the sprints that mattered. Written once
-- and never again, because a commitment that can be edited after the fact is not
-- a commitment - work added mid-sprint has to show up as a miss, which is
-- precisely the signal.
--
-- `committed_at` is kept alongside it so the number can be told apart from a
-- zero: a sprint with `committed_at` set and `committed_points` zero was started
-- empty, while a sprint with both null was never started at all. Nothing can
-- distinguish those from the points column on its own.
--
-- Both stay null on sprints started before this migration. Those sprints report
-- a delivered figure and no commitment, which is honest - nobody wrote down what
-- was promised, and this migration is not able to invent it.
