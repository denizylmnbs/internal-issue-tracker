import { useDroppable } from "@dnd-kit/core";
import { cn } from "@/lib/utils";
import { IssueStatusChip } from "@/components/shell/chips";
import { IssueCard } from "./IssueCard";
import type { IssueResponse } from "@/lib/api/types";
import type { IssueStatus } from "@/lib/api/enums";

export function BoardColumn({
  status,
  issues,
  projectId,
}: {
  status: IssueStatus;
  issues: IssueResponse[];
  projectId: number;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: status });
  const points = issues.reduce((sum, i) => sum + (i.storyPoint ?? 0), 0);

  return (
    <div className="flex w-72 shrink-0 flex-col overflow-hidden rounded border border-rule">
      <div className="flex items-center justify-between border-b border-rule bg-secondary px-2 py-1.5">
        <IssueStatusChip status={status} />
        <span className="font-data text-xs text-slate">
          {issues.length} · {points}pt
        </span>
      </div>
      <div
        ref={setNodeRef}
        className={cn(
          "flex min-h-24 flex-1 flex-col gap-2 p-2 transition-colors",
          isOver && "bg-accent",
        )}
      >
        {issues.length === 0 ? (
          <p className="p-2 text-xs text-slate">Empty</p>
        ) : (
          issues.map((issue) => <IssueCard key={issue.id} issue={issue} projectId={projectId} />)
        )}
      </div>
    </div>
  );
}
