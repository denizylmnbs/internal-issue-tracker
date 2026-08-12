"use client";

import { useCallback } from "react";
import { cn } from "@/lib/utils";
import { Skeleton } from "@/components/ui/skeleton";
import { useSprintForecast } from "@/lib/hooks/useSprintForecast";
import { useProjectContext } from "@/lib/project/ProjectContext";
import type { ForecastGap, SprintForecast as Forecast } from "@/lib/sprint/forecast";
import type { SprintResponse } from "@/lib/api/types";

/**
 * Whether a sprint is going to land, stated the way the numbers actually
 * support it: a band, not a fabricated percentage. The evidence sits next to
 * the verdict — points left, days left, the average it was measured against —
 * so a reader can disagree with it on the spot.
 *
 * Colour is work state here, which is what the palette reserves it for (see
 * app/globals.css).
 */

const VERDICT: Record<
  Exclude<Forecast, { kind: "gap" }>["verdict"],
  { label: string; dot: string; text: string }
> = {
  DONE: { label: "All done", dot: "bg-moss", text: "text-moss" },
  ON_TRACK: { label: "On track", dot: "bg-moss", text: "text-moss" },
  TIGHT: { label: "Tight", dot: "bg-amber", text: "text-amber" },
  BEHIND: { label: "Won't make it", dot: "bg-rust", text: "text-rust" },
};

const GAP_LABEL: Record<ForecastGap, string> = {
  NO_HISTORY: "No forecast yet",
  NOT_ESTIMATED: "Not estimated",
};

const GAP_DETAIL: Record<ForecastGap, string> = {
  NO_HISTORY: "Needs at least two finished sprints to measure a velocity against.",
  NOT_ESTIMATED: "Work is still open but none of it carries a story point estimate.",
};

const round = (n: number) => Math.round(n * 10) / 10;

/** The one-line summary both variants put after the verdict. */
function evidence(forecast: Extract<Forecast, { kind: "forecast" }>): string {
  if (forecast.verdict === "DONE") {
    return "Nothing left open in this sprint.";
  }

  const days =
    forecast.remainingDays === 0
      ? "past its end date"
      : `${forecast.remainingDays} ${forecast.remainingDays === 1 ? "day" : "days"} left`;

  return `${forecast.remainingPoints} pt open · ${days} · averaging ${round(
    forecast.averageVelocity,
  )} pt over the last ${forecast.sampleSize} sprint${forecast.sampleSize === 1 ? "" : "s"}`;
}

function capacityLine(forecast: Extract<Forecast, { kind: "forecast" }>): string | null {
  if (forecast.verdict === "DONE") return null;

  if (!Number.isFinite(forecast.ratio)) {
    return forecast.remainingDays === 0
      ? "No time left to absorb it."
      : "Recent sprints delivered nothing to forecast from.";
  }

  return `${Math.round(forecast.ratio * 100)}% of the ${round(
    forecast.capacity,
  )} pt this team would normally deliver in the time remaining`;
}

export function SprintForecastBanner({
  projectId,
  sprint,
}: {
  projectId: number;
  sprint: SprintResponse | undefined;
}) {
  const { forecast, isLoading } = useSprintForecast(projectId, sprint);

  if (isLoading) return <Skeleton className="h-14 w-full" />;
  if (!forecast) return null;

  if (forecast.kind === "gap") {
    return (
      <div className="rounded border border-rule px-3 py-2">
        <p className="flex items-center gap-1.5 text-sm font-medium text-slate">
          <span className="h-1.5 w-1.5 rounded-full bg-slate" />
          {GAP_LABEL[forecast.reason]}
        </p>
        <p className="mt-0.5 font-data text-xs text-slate">{GAP_DETAIL[forecast.reason]}</p>
      </div>
    );
  }

  const verdict = VERDICT[forecast.verdict];
  const capacity = capacityLine(forecast);

  return (
    <div className="rounded border border-rule px-3 py-2">
      <p className={cn("flex items-center gap-1.5 text-sm font-medium", verdict.text)}>
        <span className={cn("h-1.5 w-1.5 rounded-full", verdict.dot)} />
        {verdict.label}
      </p>
      <p className="mt-0.5 font-data text-xs text-slate">{evidence(forecast)}</p>
      {capacity && <p className="font-data text-xs text-slate">{capacity}</p>}
      {forecast.unestimatedIssues > 0 && (
        <p className="font-data text-xs text-amber">
          {forecast.unestimatedIssues} open{" "}
          {forecast.unestimatedIssues === 1 ? "issue carries" : "issues carry"} no estimate and
          {forecast.unestimatedIssues === 1 ? " is" : " are"} not counted above.
        </p>
      )}
    </div>
  );
}

/** The row-sized variant — verdict and points only, with the rest on hover. */
export function SprintForecastBadge({
  projectId,
  sprint,
}: {
  projectId: number;
  sprint: SprintResponse;
}) {
  const { forecast, isLoading } = useSprintForecast(projectId, sprint);

  if (isLoading) return <Skeleton className="h-4 w-20" />;
  if (!forecast) return null;

  if (forecast.kind === "gap") {
    return (
      <span className="font-data text-xs text-slate" title={GAP_DETAIL[forecast.reason]}>
        {GAP_LABEL[forecast.reason]}
      </span>
    );
  }

  const verdict = VERDICT[forecast.verdict];

  return (
    <span
      className={cn("inline-flex items-center gap-1.5 text-xs font-medium", verdict.text)}
      title={[evidence(forecast), capacityLine(forecast)].filter(Boolean).join(" — ")}
    >
      <span className={cn("h-1.5 w-1.5 rounded-full", verdict.dot)} />
      {verdict.label}
      {forecast.verdict !== "DONE" && (
        <span className="font-data text-slate">{forecast.remainingPoints} pt</span>
      )}
    </span>
  );
}

/** Sprints that have already been closed out get no forecast — there is
 * nothing left to predict, and velocity is the honest measure by then.
 * `"COMPLETED"` was a hardcoded literal before sprint statuses became
 * project-defined data; a sprint is "closed out" now if its status carries
 * this project's SPRINT_STATUS `isDone` flag. Must be called within a
 * ProjectProvider. */
export function useIsForecastable() {
  const { fieldDefinitionsByKind } = useProjectContext();

  return useCallback(
    (sprint: SprintResponse) => {
      const doneCodes = fieldDefinitionsByKind
        .get("SPRINT_STATUS")
        ?.filter((d) => d.isDone)
        .map((d) => d.code);
      return !doneCodes?.includes(sprint.status);
    },
    [fieldDefinitionsByKind],
  );
}
