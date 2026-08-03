import { isSameDay } from "date-fns";
import { ActivityRow } from "./ActivityRow";
import { EmptyState } from "@/components/shell/EmptyState";
import type { ActivityResponse } from "@/lib/api/types";

/** Activity-only spine — the project activity feed and the sprint activity
 * view. The issue page builds its own richer version that interleaves
 * comments, reusing SpineRow directly (see components/issue/IssueSpine.tsx). */
export function ActivitySpine({
  activities,
  domain,
  projectId,
}: {
  activities: ActivityResponse[];
  domain: "issue" | "sprint" | "project";
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
            key={activity.id}
            activity={activity}
            domain={domain}
            projectId={projectId}
            showDate={showDate}
          />
        );
      })}
    </div>
  );
}
