import { useMemo } from "react";
import { useVelocity } from "./useMetrics";
import { useIssuesList } from "./useIssues";
import { sprintForecast, type SprintForecast } from "@/lib/sprint/forecast";
import type { SprintResponse } from "@/lib/api/types";

/**
 * The two reads a forecast needs, and no new endpoint: `metrics/velocity`
 * already returns every sprint with its delivered points, and the sprint's
 * remaining work is the issue list the board is looking at anyway.
 *
 * The issue query deliberately matches the board's own arguments exactly — same
 * sprint, same size, same sort — so on the busiest page that shows this, the two
 * hooks share one cache entry and one request rather than racing for the same
 * rows under different keys.
 *
 * Remaining work is summed from the live issues rather than taken from the
 * sprint's `committedPoints`, which is frozen at the moment the sprint starts
 * (see `Sprint#committedPoints`) and by design does not know about anything
 * pulled in since.
 */
export function useSprintForecast(projectId: number, sprint: SprintResponse | undefined) {
  const velocity = useVelocity(projectId);
  const issues = useIssuesList(projectId, {
    sprintId: sprint?.id,
    size: 200,
    sort: "priority,desc",
  });

  const forecast = useMemo<SprintForecast | undefined>(() => {
    if (!sprint || !velocity.data || !issues.data) return undefined;

    return sprintForecast({
      sprint,
      issues: issues.data.content,
      history: velocity.data.sprints,
    });
  }, [sprint, velocity.data, issues.data]);

  return { forecast, isLoading: velocity.isLoading || issues.isLoading };
}
