"use client";

import { useState, useEffect } from "react";
import { DndContext, type DragEndEvent } from "@dnd-kit/core";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useSprintsList } from "@/lib/hooks/useSprints";
import { useIssuesList, useChangeIssueStatus } from "@/lib/hooks/useIssues";
import { BoardColumn } from "@/components/board/BoardColumn";
import { EmptyState } from "@/components/shell/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { BOARD_STATUSES } from "@/lib/api/enums";
import type { IssueStatus } from "@/lib/api/enums";
import Link from "next/link";

export default function BoardPage() {
  const { projectId } = useProjectContext();
  const { data: sprints, isLoading: loadingSprints } = useSprintsList(projectId, {
    size: 100,
    sort: "startDate,desc",
  });
  const [sprintId, setSprintId] = useState<number | undefined>();

  useEffect(() => {
    if (sprintId != null || !sprints?.content.length) return;
    const running = sprints.content.find((s) => s.status === "IN_PROGRESS");
    setSprintId(running?.id ?? sprints.content[0].id);
  }, [sprints, sprintId]);

  const { data: issues, isLoading: loadingIssues } = useIssuesList(projectId, {
    sprintId,
    size: 200,
    sort: "priority,desc",
  });
  const changeStatus = useChangeIssueStatus(projectId);

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
    const newStatus = over.id as IssueStatus;
    const current = issues?.content.find((i) => i.id === issueId);
    if (!current || current.status === newStatus) return;
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
                {s.name} {s.status === "IN_PROGRESS" && "· running"}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {loadingIssues || !sprintId ? (
        <div className="flex gap-3">
          {BOARD_STATUSES.map((s) => (
            <Skeleton key={s} className="h-96 w-72" />
          ))}
        </div>
      ) : (
        <DndContext onDragEnd={onDragEnd}>
          <div className="flex flex-1 gap-4 overflow-x-auto pb-2">
            {BOARD_STATUSES.map((status) => (
              <BoardColumn
                key={status}
                status={status}
                projectId={projectId}
                issues={(issues?.content ?? []).filter((i) => i.status === status)}
              />
            ))}
          </div>
        </DndContext>
      )}
    </div>
  );
}
