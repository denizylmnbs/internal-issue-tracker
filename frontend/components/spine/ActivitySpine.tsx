import { isSameDay } from "date-fns";
import { ActivityRow } from "./ActivityRow";
import { EmptyState } from "@/components/shell/EmptyState";
import type { ActivityResponse } from "@/lib/api/types";

/** Activity-only spine — the project activity feed and the sprint activity
 * view. The issue page builds its own richer version that interleaves
 * comments, reusing SpineRow directly (see components/issue/IssueSpine.tsx).
 *
 * No `domain` prop: `ActivityRow` derives it per-row from `activity.scope`
 * (docs/API.md §4.12), which matters here specifically because the project
 * feed is a union of project/issue/sprint history — a single fixed domain
 * would mis-decode two of the three kinds of row it can now contain. */
export function ActivitySpine({
  activities,
  projectId,
}: {
  activities: ActivityResponse[];
  projectId: number;
}) {
  if (activities.length === 0) {
    return (
      <EmptyState
        title="Nothing here yet"
        description="Changes will appear here the moment something moves."
      />
    );
  }

  return (
    <div>
      {activities.map((activity, i) => {
        const prev = activities[i - 1];
        const showDate = !prev || !isSameDay(new Date(prev.createdAt), new Date(activity.createdAt));
        return (
          <ActivityRow
            // `id` is only unique within one activity table — the union feed
            // can hand back an ISSUE row and a SPRINT row that share an id.
            key={`${activity.scope}-${activity.id}`}
            activity={activity}
            projectId={projectId}
            showDate={showDate}
          />
        );
      })}
    </div>
  );
}
