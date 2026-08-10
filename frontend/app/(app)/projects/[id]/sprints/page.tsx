"use client";

import { useState } from "react";
import Link from "next/link";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useSprintsList, useDeleteSprint } from "@/lib/hooks/useSprints";
import { SprintStatusSelect } from "@/components/pickers/SprintStatusSelect";
import { SprintFormDialog } from "@/components/pickers/SprintFormDialog";
import { EmptyState } from "@/components/shell/EmptyState";
import { SprintForecastBadge, isForecastable } from "@/components/sprint/SprintForecast";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { formatDateOnly } from "@/lib/format";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import type { SprintResponse } from "@/lib/api/types";

export default function SprintsPage() {
  const { projectId, canManage } = useProjectContext();
  const { data, isLoading } = useSprintsList(projectId, { sort: "startDate,desc" });
  const deleteSprint = useDeleteSprint(projectId);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<SprintResponse | undefined>();

  const openCreate = () => {
    setEditing(undefined);
    setFormOpen(true);
  };
  const openEdit = (s: SprintResponse) => {
    setEditing(s);
    setFormOpen(true);
  };

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-heading text-xl font-semibold tracking-tight">Sprints</h1>
        {canManage && (
          <Button size="sm" onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New sprint
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : !data?.content.length ? (
        <EmptyState
          title="No sprints yet"
          description="A sprint is what a board is built around — create the first one to start planning."
        />
      ) : (
        <div className="divide-y divide-rule rounded border border-rule">
          {data.content.map((sprint) => (
            <div key={sprint.id} className="flex items-center justify-between gap-4 p-3">
              <div className="min-w-0 flex-1">
                <Link
                  href={`/projects/${projectId}/board?sprintId=${sprint.id}`}
                  className="text-sm font-medium hover:text-signal"
                >
                  {sprint.name}
                </Link>
                <p className="mt-0.5 font-data text-xs text-slate">
                  {formatDateOnly(sprint.startDate)} – {formatDateOnly(sprint.endDate)}
                  {sprint.committedPoints != null && (
                    <span> · committed {sprint.committedPoints}pt</span>
                  )}
                </p>
              </div>
              {/* Only for sprints still in play — a finished one is measured,
                  not forecast, and this is what keeps the list from firing an
                  issue query per closed sprint. */}
              {isForecastable(sprint) && (
                <SprintForecastBadge projectId={projectId} sprint={sprint} />
              )}
              <SprintStatusSelect
                projectId={projectId}
                sprintId={sprint.id}
                status={sprint.status}
                disabled={!canManage}
              />
              {canManage && (
                <div className="flex gap-1">
                  <Button variant="ghost" size="icon" onClick={() => openEdit(sprint)}>
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Delete "{sprint.name}"?</AlertDialogTitle>
                        <AlertDialogDescription>
                          Issues in this sprint keep their sprint reference and will show as
                          pointing at a deleted sprint — deletion doesn't cascade.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={() => deleteSprint.mutate(sprint.id)}>
                          Delete
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {formOpen && (
        <SprintFormDialog
          open={formOpen}
          onOpenChange={setFormOpen}
          projectId={projectId}
          sprint={editing}
        />
      )}
    </div>
  );
}
