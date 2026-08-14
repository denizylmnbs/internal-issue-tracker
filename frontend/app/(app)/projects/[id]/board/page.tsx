"use client";

import { useState, useEffect, useMemo, Suspense, type CSSProperties } from "react";
import { useSearchParams } from "next/navigation";
import { DndContext, type DragEndEvent } from "@dnd-kit/core";
import { cn } from "@/lib/utils";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useSession } from "@/lib/auth/session";
import { canWriteIssue } from "@/lib/auth/can";
import { useSprintsList } from "@/lib/hooks/useSprints";
import { useIssuesList, useChangeIssueStatus } from "@/lib/hooks/useIssues";
import { BoardColumn } from "@/components/board/BoardColumn";
import { SprintForecastBanner, useIsForecastable } from "@/components/sprint/SprintForecast";
import { EmptyState } from "@/components/shell/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import Link from "next/link";

/**
 * Every visible status shares the width equally from `md` up, so the whole
 * board fits the screen however many statuses a project defines. It used to be
 * fixed 288px columns in a horizontally scrolling flex row, where a project
 * with more than four or five statuses lost the rest off the right edge.
 *
 * `minmax(0,1fr)` rather than a bare `1fr`: without the zero floor a grid track
 * refuses to shrink below its own content and the row overflows anyway. What
 * holds up once a column is narrow is the card's problem - see `IssueCard`.
 *
 * Below `md` the fit is dropped for a 13.5rem floor and sideways scrolling.
 * Seven columns on a phone would be 50px each, which is not a board any more -
 * scrolling a readable one beats fitting an unusable one at that width.
 */
const BOARD_GRID =
  "grid gap-3 grid-cols-[repeat(var(--board-cols),minmax(13.5rem,1fr))] overflow-x-auto md:grid-cols-[repeat(var(--board-cols),minmax(0,1fr))] md:overflow-x-visible";

function BoardPageContent() {
  const { projectId, project, fieldDefinitionsByKind } = useProjectContext();
  const isForecastable = useIsForecastable();
  const { user } = useSession();
  const searchParams = useSearchParams();
  const requestedSprintId = searchParams.get("sprintId");
  const { data: sprints, isLoading: loadingSprints } = useSprintsList(projectId, {
    size: 100,
    sort: "startDate,desc",
  });
  const [sprintId, setSprintId] = useState<number | undefined>(
    requestedSprintId ? Number(requestedSprintId) : undefined,
  );

  // The columns a board renders, left to right: this project's active
  // ISSUE_STATUS rows, minus the default (backlog-ish) and cancelled ones -
  // docs/API.md §2. Used to be a hardcoded TODO/IN_PROGRESS/IN_REVIEW/DONE
  // subset; any status a project defines with neither flag now shows up here.
  const columns = (fieldDefinitionsByKind.get("ISSUE_STATUS") ?? []).filter(
    (d) => !d.isDefault && !d.isCancelled,
  );
  // "IN_PROGRESS" was a hardcoded literal — SPRINT_STATUS codes flagged
  // isActiveWork are this project's "currently running" set now.
  const sprintStatusDefs = useMemo(
    () => fieldDefinitionsByKind.get("SPRINT_STATUS") ?? [],
    [fieldDefinitionsByKind],
  );
  const runningStatuses = new Set(
    sprintStatusDefs.filter((d) => d.isActiveWork).map((d) => d.code),
  );

  useEffect(() => {
    if (sprintId != null || !sprints?.content.length) return;
    if (requestedSprintId) {
      const requested = Number(requestedSprintId);
      if (sprints.content.some((s) => s.id === requested)) {
        setSprintId(requested);
        return;
      }
    }
    const running = sprints.content.find((s) =>
      sprintStatusDefs.some((d) => d.isActiveWork && d.code === s.status),
    );
    setSprintId(running?.id ?? sprints.content[0].id);
  }, [sprints, sprintId, requestedSprintId, sprintStatusDefs]);

  const { data: issues, isLoading: loadingIssues } = useIssuesList(projectId, {
    sprintId,
    size: 200,
    sort: "priority,desc",
  });
  const changeStatus = useChangeIssueStatus(projectId);

  // How many columns there are is data, so the count travels as a CSS custom
  // property and the track sizing stays in classes - a Tailwind class cannot be
  // assembled from a runtime value and still exist in the stylesheet.
  const boardVars = { "--board-cols": String(Math.max(columns.length, 1)) } as CSSProperties;

  const selected = sprints?.content.find((s) => s.id === sprintId);
  // a closed-out sprint has nothing left to predict — see isForecastable
  const selectedSprint = selected && isForecastable(selected) ? selected : undefined;

  if (!loadingSprints && sprints?.content.length === 0) {
    return (
      <div className="p-6">
        <EmptyState
          title="No sprints yet"
          description="Create a sprint to start a board."
          action={
            <Link href={`/projects/${projectId}/sprints`} className="text-sm text-signal hover:underline">
              Go to sprints →
            </Link>
          }
        />
      </div>
    );
  }

  const onDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over) return;
    const issueId = Number(active.id);
    const newStatus = over.id as string;
    const current = issues?.content.find((i) => i.id === issueId);
    if (!current || current.status === newStatus) return;
    // Editor / project leader / the issue's own assignee only — a
    // participant who is neither cannot move someone else's issue. Cards
    // are also non-draggable for them (see IssueCard's `disabled`), this is
    // the belt-and-suspenders check for anything that slips past that.
    if (!canWriteIssue(user, project?.leaderId, current)) return;
    changeStatus.mutate({ issueId, body: { status: newStatus } });
  };

  return (
    <div className="flex h-full flex-col p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-heading text-xl font-semibold tracking-tight">Board</h1>
        <Select
          value={sprintId ? String(sprintId) : undefined}
          onValueChange={(v) => setSprintId(Number(v))}
        >
          <SelectTrigger className="w-56">
            <SelectValue placeholder="Select a sprint" />
          </SelectTrigger>
          <SelectContent>
            {sprints?.content.map((s) => (
              <SelectItem key={s.id} value={String(s.id)}>
                {s.name} {runningStatuses.has(s.status) && "· running"}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {selectedSprint && (
        <div className="mb-4">
          <SprintForecastBanner projectId={projectId} sprint={selectedSprint} />
        </div>
      )}

      {loadingIssues || !sprintId ? (
        <div className={BOARD_GRID} style={boardVars}>
          {columns.map((s) => (
            <Skeleton key={s.code} className="h-96 w-full" />
          ))}
        </div>
      ) : (
        <DndContext onDragEnd={onDragEnd}>
          <div className={cn(BOARD_GRID, "min-h-0 flex-1 pb-2")} style={boardVars}>
            {columns.map((status) => (
              <BoardColumn
                key={status.code}
                status={status}
                projectId={projectId}
                issues={(issues?.content ?? []).filter((i) => i.status === status.code)}
                canWriteIssue={(issue) => canWriteIssue(user, project?.leaderId, issue)}
                currentUserId={user?.id}
              />
            ))}
          </div>
        </DndContext>
      )}
    </div>
  );
}

export default function BoardPage() {
  return (
    <Suspense>
      <BoardPageContent />
    </Suspense>
  );
}
