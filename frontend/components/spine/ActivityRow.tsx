import { SpineRow } from "./SpineRow";
import { ValueDelta } from "./ValueDelta";
import { UserName } from "@/lib/users/directory";
import { ACTION_LABEL } from "@/lib/api/enums";
import type { ActivityResponse } from "@/lib/api/types";

const DOT_FOR_ACTION: Record<string, string> = {
  CREATED: "bg-moss",
  DELETED: "bg-rust",
};

export function ActivityRow({
  activity,
  domain,
  projectId,
  showDate,
}: {
  activity: ActivityResponse;
  domain: "issue" | "sprint" | "project";
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
          domain={domain}
          projectId={projectId}
        />
      </div>
    </SpineRow>
  );
}
