import { SpineRow } from "./SpineRow";
import { ValueDelta } from "./ValueDelta";
import { UserName } from "@/lib/users/directory";
import { ACTION_LABEL } from "@/lib/api/enums";
import type { ActivityResponse } from "@/lib/api/types";

const DOT_FOR_ACTION: Record<string, string> = {
  CREATED: "bg-moss",
  DELETED: "bg-rust",
};

/** `activity.scope` (docs/API.md §4.12) is present on every row from every
 * activity endpoint, including the unioned project feed — so it, not a
 * page-level prop, is what tells `ValueDelta` how to decode a shared action
 * type like `STATUS_UPDATED` (issue/sprint/project all have one). */
const DOMAIN_FOR_SCOPE: Record<string, "issue" | "sprint" | "project"> = {
  ISSUE: "issue",
  SPRINT: "sprint",
  PROJECT: "project",
};

export function ActivityRow({
  activity,
  projectId,
  showDate,
}: {
  activity: ActivityResponse;
  projectId: number;
  showDate?: boolean;
}) {
  return (
    <SpineRow
      timestamp={activity.createdAt}
      showDate={showDate}
      dotClassName={DOT_FOR_ACTION[activity.actionType] ?? "bg-slate"}
    >
      <p className="text-sm">
        <span className="font-medium">
          <UserName id={activity.userId} />
        </span>{" "}
        <span className="text-slate">{ACTION_LABEL[activity.actionType] ?? activity.actionType}</span>
      </p>
      <div className="mt-0.5">
        <ValueDelta
          actionType={activity.actionType}
          oldValue={activity.oldValue}
          newValue={activity.newValue}
          domain={DOMAIN_FOR_SCOPE[activity.scope] ?? "issue"}
          projectId={projectId}
        />
      </div>
    </SpineRow>
  );
}
