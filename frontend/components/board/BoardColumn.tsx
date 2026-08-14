import { useDroppable } from "@dnd-kit/core";
import { cn } from "@/lib/utils";
import { IssueStatusChip } from "@/components/shell/chips";
import { IssueCard } from "./IssueCard";
import type { IssueResponse } from "@/lib/api/types";
import type { FieldDefinitionResponse } from "@/lib/api/types";

export function BoardColumn({
  status,
  issues,
  projectId,
  canWriteIssue,
  currentUserId,
}: {
  /** The ISSUE_STATUS field definition this column renders — its `code` is
   * both the droppable id and what a dropped card's status is set to. */
  status: FieldDefinitionResponse;
  issues: IssueResponse[];
  projectId: number;
  /** Editor / project leader / the issue's own assignee — see lib/auth/can.ts. */
  canWriteIssue: (issue: IssueResponse) => boolean;
  /** Highlights cards assigned to the signed-in user. */
  currentUserId?: number;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: status.code });
  const points = issues.reduce((sum, i) => sum + (i.storyPoint ?? 0), 0);

  return (
    // no fixed width: the page grid hands every column an equal share and the
    // column takes it, down to whatever a project's status count leaves over
    <div className="flex min-w-0 flex-col overflow-hidden rounded border border-rule">
      {/* the status label truncates before the count does - a column whose
          issue count is cut off says nothing at all */}
      <div className="flex items-center justify-between gap-1 border-b border-rule bg-secondary px-2 py-1.5">
        <IssueStatusChip status={status.code} />
        <span
          className="shrink-0 font-data text-xs text-slate"
          title={`${issues.length} issues · ${points} points`}
        >
          {issues.length} · {points}pt
        </span>
      </div>
      <div
        ref={setNodeRef}
        className={cn(
          "flex min-h-24 flex-1 flex-col gap-2 overflow-y-auto p-2 transition-colors",
          isOver && "bg-accent",
        )}
      >
        {issues.length === 0 ? (
          <p className="p-2 text-xs text-slate">Empty</p>
        ) : (
          issues.map((issue) => (
            <IssueCard
              key={issue.id}
              issue={issue}
              projectId={projectId}
              disabled={!canWriteIssue(issue)}
              currentUserId={currentUserId}
            />
          ))
        )}
      </div>
    </div>
  );
}
