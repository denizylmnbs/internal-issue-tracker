"use client";

import { useState } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useEpicsList, useDeleteEpic } from "@/lib/hooks/useEpics";
import { EpicStatusSelect } from "@/components/pickers/EpicStatusSelect";
import { EpicFormDialog } from "@/components/pickers/EpicFormDialog";
import { EmptyState } from "@/components/shell/EmptyState";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { UserName } from "@/lib/users/directory";
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
import type { EpicResponse } from "@/lib/api/types";

export default function EpicsPage() {
  const { projectId, canManage } = useProjectContext();
  const { data, isLoading } = useEpicsList(projectId, { sort: "createdAt,desc" });
  const deleteEpic = useDeleteEpic(projectId);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<EpicResponse | undefined>();

  const openCreate = () => {
    setEditing(undefined);
    setFormOpen(true);
  };
  const openEdit = (e: EpicResponse) => {
    setEditing(e);
    setFormOpen(true);
  };

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-heading text-xl font-semibold tracking-tight">Epics</h1>
        {canManage && (
          <Button size="sm" onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New epic
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
          title="No epics yet"
          description="Group related issues under an epic to track them together."
        />
      ) : (
        <div className="divide-y divide-rule rounded border border-rule">
          {data.content.map((epic) => (
            <div key={epic.id} className="flex items-start justify-between gap-4 p-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium">{epic.name}</p>
                {epic.description && (
                  <p className="mt-0.5 line-clamp-2 text-xs text-slate">{epic.description}</p>
                )}
                <p className="mt-1 text-xs text-slate">
                  Reported by <UserName id={epic.reporterId} />
                </p>
              </div>
              <EpicStatusSelect
                projectId={projectId}
                epicId={epic.id}
                status={epic.status}
                disabled={!canManage}
              />
              {canManage && (
                <div className="flex gap-1">
                  <Button variant="ghost" size="icon" onClick={() => openEdit(epic)}>
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
                        <AlertDialogTitle>Delete "{epic.name}"?</AlertDialogTitle>
                        <AlertDialogDescription>
                          Issues under this epic keep their epic reference and will show as
                          pointing at a deleted epic — deletion doesn't cascade.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={() => deleteEpic.mutate(epic.id)}>
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
        <EpicFormDialog open={formOpen} onOpenChange={setFormOpen} projectId={projectId} epic={editing} />
      )}
    </div>
  );
}
