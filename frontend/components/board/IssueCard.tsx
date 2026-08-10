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
      <div className="mb-1.5 flex items-center justify-between">
        <span className="font-data text-[11px] text-slate">ISS-{issue.id}</span>
        <TypeChip type={issue.type} />
      </div>
      <Link
        href={`/projects/${projectId}/issues/${issue.id}`}
        onClick={(e) => e.stopPropagation()}
        className="block text-sm leading-snug hover:text-signal"
      >
        {issue.name}
      </Link>
      <div className="mt-2 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <PriorityChip priority={issue.priority} />
          {issue.resolvingUnit != null && <UnitChip unit={issue.resolvingUnit} />}
        </div>
        <div className="flex items-center gap-2">
          {issue.storyPoint != null && (
            <span className="font-data text-xs text-slate">{issue.storyPoint}</span>
          )}
          <span className="text-xs text-slate">
            <UserName id={issue.assigneeUserId} />
          </span>
        </div>
      </div>
    </div>
  );
}
