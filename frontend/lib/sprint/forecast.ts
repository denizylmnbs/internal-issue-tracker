import { differenceInCalendarDays, parseISO } from "date-fns";

/**
 * Will this sprint land? Answered from two numbers the project already
 * publishes: what is still open in the sprint, and how much this team has
 * actually delivered per sprint lately.
 *
 * Deliberately free of React and of the API client — it takes plain values and
 * returns a verdict, so its behaviour can be reasoned about (and tested) without
 * a query cache or a rendered tree.
 */

/** How many finished sprints the average is taken over. */
export const VELOCITY_SAMPLE_SIZE = 6;

/** One finished sprint is an anecdote; below this we decline to guess. */
const MIN_SAMPLES = 2;

/** Comfortably inside the forecast capacity. */
const ON_TRACK_RATIO = 0.8;

export type ForecastVerdict = "DONE" | "ON_TRACK" | "TIGHT" | "BEHIND";

/** Why no forecast could be produced — each reads differently to a user, so
 * none of them collapses into a generic "unavailable". */
export type ForecastGap = "NO_HISTORY" | "NOT_ESTIMATED";

export type SprintForecast =
  | { kind: "gap"; reason: ForecastGap; sampleSize: number }
  | {
      kind: "forecast";
      verdict: ForecastVerdict;
      /** Story points on issues that are neither done nor cancelled. */
      remainingPoints: number;
      /** Mean delivered points across the sampled finished sprints. */
      averageVelocity: number;
      /** Velocity pro-rated to the part of the sprint that is left. */
      capacity: number;
      /** remainingPoints / capacity. `Infinity` once capacity reaches zero. */
      ratio: number;
      sampleSize: number;
      totalDays: number;
      remainingDays: number;
      /** Open issues carrying no estimate — they weigh nothing here, which is
       * exactly why the count has to be shown alongside the verdict. */
      unestimatedIssues: number;
    };

export type ForecastSprint = {
  startDate: string;
  endDate: string;
};

export type ForecastIssue = {
  status: string;
  storyPoint: number | null;
};

export type ForecastHistoryEntry = {
  status: string;
  endDate: string;
  completedPoints: number | null;
};

/**
 * The average is taken over finished sprints only. A sprint still running has
 * no final delivery figure, and counting its partial total would drag the
 * average down by exactly the amount the team has not had time to finish yet.
 *
 * `doneSprintStatuses` is the calling project's set of SPRINT_STATUS codes
 * flagged `isDone` (docs/API.md §2) — "COMPLETED" was a hardcoded literal
 * before sprint statuses became project-defined data.
 */
function recentVelocities(
  history: ForecastHistoryEntry[],
  doneSprintStatuses: ReadonlySet<string>,
): number[] {
  return history
    .filter((s) => doneSprintStatuses.has(s.status) && s.completedPoints != null)
    .sort((a, b) => b.endDate.localeCompare(a.endDate))
    .slice(0, VELOCITY_SAMPLE_SIZE)
    .map((s) => s.completedPoints as number);
}

/**
 * Both ends inclusive, and both read as calendar days in the viewer's zone:
 * the backend sends `LocalDate`, so `parseISO` is what keeps a sprint ending
 * "Jan 15" from becoming Jan 14 for anyone west of UTC.
 */
function dayCounts(sprint: ForecastSprint, now: Date) {
  const start = parseISO(sprint.startDate);
  const end = parseISO(sprint.endDate);

  const totalDays = Math.max(1, differenceInCalendarDays(end, start) + 1);
  const remainingDays = Math.min(
    totalDays,
    Math.max(0, differenceInCalendarDays(end, now) + 1),
  );

  return { totalDays, remainingDays };
}

export function sprintForecast({
  sprint,
  issues,
  history,
  closedIssueStatuses,
  doneSprintStatuses,
  now = new Date(),
}: {
  sprint: ForecastSprint;
  issues: ForecastIssue[];
  history: ForecastHistoryEntry[];
  /** This project's ISSUE_STATUS codes flagged `isDone` or `isCancelled` — work
   * carrying one of these is no longer "remaining". */
  closedIssueStatuses: ReadonlySet<string>;
  /** This project's SPRINT_STATUS codes flagged `isDone`. */
  doneSprintStatuses: ReadonlySet<string>;
  now?: Date;
}): SprintForecast {
  const velocities = recentVelocities(history, doneSprintStatuses);
  const sampleSize = velocities.length;

  const open = issues.filter((i) => !closedIssueStatuses.has(i.status));
  const remainingPoints = open.reduce((sum, i) => sum + (i.storyPoint ?? 0), 0);
  const unestimatedIssues = open.filter((i) => i.storyPoint == null).length;

  const { totalDays, remainingDays } = dayCounts(sprint, now);

  // nothing left to do outranks every other answer, including a missing history
  if (open.length === 0) {
    return {
      kind: "forecast",
      verdict: "DONE",
      remainingPoints: 0,
      averageVelocity: sampleSize > 0 ? mean(velocities) : 0,
      capacity: 0,
      ratio: 0,
      sampleSize,
      totalDays,
      remainingDays,
      unestimatedIssues: 0,
    };
  }

  if (sampleSize < MIN_SAMPLES) {
    return { kind: "gap", reason: "NO_HISTORY", sampleSize };
  }

  // work is left, but none of it is estimated — reporting 0 points remaining
  // would read as "on track" when the truth is that nobody has sized it
  if (remainingPoints === 0) {
    return { kind: "gap", reason: "NOT_ESTIMATED", sampleSize };
  }

  const averageVelocity = mean(velocities);
  const capacity = averageVelocity * (remainingDays / totalDays);

  // past the end date, or a team whose recent sprints delivered nothing: there
  // is no capacity left to finish the work in, and no ratio to express it as
  const ratio = capacity > 0 ? remainingPoints / capacity : Infinity;

  return {
    kind: "forecast",
    verdict: ratio <= ON_TRACK_RATIO ? "ON_TRACK" : ratio <= 1 ? "TIGHT" : "BEHIND",
    remainingPoints,
    averageVelocity,
    capacity,
    ratio,
    sampleSize,
    totalDays,
    remainingDays,
    unestimatedIssues,
  };
}

function mean(values: number[]): number {
  return values.reduce((sum, v) => sum + v, 0) / values.length;
}
