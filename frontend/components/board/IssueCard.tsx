import { useDraggable } from "@dnd-kit/core";
import { CSS } from "@dnd-kit/utilities";
import { cn } from "@/lib/utils";
import { TypeChip, PriorityChip, UnitChip } from "@/components/shell/chips";
import { UserName } from "@/lib/users/directory";
import type { IssueResponse } from "@/lib/api/types";
import Link from "next/link";

export function IssueCard({
  issue,
  projectId,
  disabled,
  currentUserId,
}: {
  issue: IssueResponse;
  projectId: number;
  /** Card can't be picked up at all when the caller may not write this issue
   * (editor / project leader / the issue's own assignee — lib/auth/can.ts). */
  disabled?: boolean;
  /** Highlights the card when it's assigned to the signed-in user. */
  currentUserId?: number;
}) {
  const isMine = currentUserId != null && issue.assigneeUserId === currentUserId;
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: issue.id,
    disabled,
  });

  const style = transform
    ? { transform: CSS.Translate.toString(transform) }
    : undefined;

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...(disabled ? {} : listeners)}
      {...attributes}
      className={cn(
        "touch-none rounded border border-rule bg-card p-2.5 hover:border-slate/40",
        disabled ? "cursor-default" : "cursor-grab active:cursor-grabbing",
        isDragging && "opacity-40",
        isMine && "border-2 border-signal shadow-sm",
      )}
    >
      {/* Everything here is written to survive a narrow column: the board
          divides the width by however many statuses a project defines, so a
          card can be a third of what it used to be. Nothing has a fixed width,
          the long strings truncate or clamp, and both meta rows wrap rather
          than pushing the card past its column. */}
      <div className="mb-1.5 flex items-center justify-between gap-1.5">
        <span className="shrink-0 font-data text-[11px] text-slate">ISS-{issue.id}</span>
        <span className="min-w-0 truncate" title={issue.type}>
          <TypeChip type={issue.type} />
        </span>
      </div>
      <Link
        href={`/projects/${projectId}/issues/${issue.id}`}
        onClick={(e) => e.stopPropagation()}
        title={issue.name}
        className="line-clamp-3 block break-words text-sm leading-snug hover:text-signal"
      >
        {issue.name}
      </Link>
      <div className="mt-2 flex flex-wrap items-center justify-between gap-x-2 gap-y-1">
        <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
          <PriorityChip priority={issue.priority} />
          {issue.resolvingUnit != null && <UnitChip unit={issue.resolvingUnit} />}
        </div>
        <div className="flex min-w-0 items-center gap-2">
          {issue.storyPoint != null && (
            <span className="shrink-0 font-data text-xs text-slate">{issue.storyPoint}</span>
          )}
          <span className="min-w-0 truncate text-xs text-slate">
            <UserName id={issue.assigneeUserId} />
          </span>
        </div>
      </div>
    </div>
  );
}
