"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Trash2, Pencil } from "lucide-react";
import { useIssue, useDeleteIssue } from "@/lib/hooks/useIssues";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useSession } from "@/lib/auth/session";
import { canDeleteIssue, canWriteIssue } from "@/lib/auth/can";
import { TypeChip, PriorityChip, UnitChip } from "@/components/shell/chips";
import { StatusControl } from "@/components/issue/StatusControl";
import { AssigneeControl } from "@/components/issue/AssigneeControl";
import { IssueSpine } from "@/components/issue/IssueSpine";
import { IssueFormDialog } from "@/components/pickers/IssueFormDialog";
import { useSprintSafe } from "@/lib/hooks/useSprints";
import { useEpicSafe } from "@/lib/hooks/useEpics";
import { UserName } from "@/lib/users/directory";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
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
import Link from "next/link";

export function IssueDetailClient({ projectId, issueId }: { projectId: number; issueId: number }) {
  const router = useRouter();
  const { user } = useSession();
  const { project, canWork } = useProjectContext();
  const { data: issue, isLoading } = useIssue(projectId, issueId);
  const deleteIssue = useDeleteIssue(projectId);
  const [editOpen, setEditOpen] = useState(false);

  const { data: sprint, deleted: sprintDeleted } = useSprintSafe(projectId, issue?.sprintId);
  const { data: epic, deleted: epicDeleted } = useEpicSafe(projectId, issue?.epicId);

  if (isLoading || !issue) {
    return (
      <div className="space-y-4 p-6">
        <Skeleton className="h-8 w-96" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  const canDelete = canDeleteIssue(user, project?.leaderId);
  // Narrower than canWork: status/assignee are editor / leader / the issue's
  // own assignee, not every project participant (docs/API.md §4.10).
  const canWriteThisIssue = canWriteIssue(user, project?.leaderId, issue);

  return (
    <div className="grid h-full grid-cols-1 lg:grid-cols-[1fr_420px]">
      <div className="min-w-0 overflow-y-auto p-6">
        <div className="mb-1 flex items-center justify-between">
          <span className="font-data text-xs text-slate">ISS-{issue.id}</span>
          <div className="flex gap-1">
            {canWork && (
              <Button variant="ghost" size="icon" onClick={() => setEditOpen(true)}>
                <Pencil className="h-4 w-4" />
              </Button>
            )}
            {canDelete && (
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button variant="ghost" size="icon">
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Delete ISS-{issue.id}?</AlertDialogTitle>
                    <AlertDialogDescription>
                      This soft-deletes the issue — it disappears from lists but its history stays intact.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction
                      onClick={() =>
                        deleteIssue.mutate(issue.id, {
                          onSuccess: () => router.push(`/projects/${projectId}/backlog`),
                        })
                      }
                    >
                      Delete
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            )}
          </div>
        </div>

        <h1 className="mb-3 font-heading text-xl font-semibold leading-snug tracking-tight">
          {issue.name}
        </h1>

        <div className="mb-5 flex flex-wrap items-center gap-3 border-b border-rule pb-4 text-sm">
          <StatusControl projectId={projectId} issueId={issue.id} status={issue.status} disabled={!canWriteThisIssue} />
          <TypeChip type={issue.type} />
          <PriorityChip priority={issue.priority} />
          {issue.resolvingUnit != null && <UnitChip unit={issue.resolvingUnit} />}
          {issue.storyPoint != null && (
            <span className="font-data text-slate">{issue.storyPoint} pts</span>
          )}
          <span className="text-slate">
            Sprint:{" "}
            {issue.sprintId == null ? (
              "backlog"
            ) : sprintDeleted ? (
              <span className="line-through">deleted</span>
            ) : (
              sprint?.name ?? "…"
            )}
          </span>
          {issue.epicId != null && (
            <span className="text-slate">
              Epic:{" "}
              {epicDeleted ? <span className="line-through">deleted</span> : epic?.name ?? "…"}
            </span>
          )}
          <span className="text-slate">
            Reported by <UserName id={issue.reporterId} />
          </span>
        </div>

        <div className="mb-5 max-w-64">
          <p className="mb-1 text-xs font-medium text-slate">Assignee</p>
          <AssigneeControl
            projectId={projectId}
            issueId={issue.id}
            assigneeUserId={issue.assigneeUserId}
            assigneeTeamId={issue.assigneeTeamId}
            disabled={!canWriteThisIssue}
          />
        </div>

        {issue.description ? (
          <p className="whitespace-pre-wrap text-sm leading-relaxed">{issue.description}</p>
        ) : (
          <p className="text-sm italic text-slate">No description.</p>
        )}
      </div>

      <div className="min-w-0 overflow-y-auto border-t border-rule p-6 lg:border-l lg:border-t-0">
        <h2 className="mb-4 font-heading text-sm font-semibold">History</h2>
        <IssueSpine projectId={projectId} issueId={issue.id} />
      </div>

      {editOpen && (
        <IssueFormDialog
          open={editOpen}
          onOpenChange={setEditOpen}
          projectId={projectId}
          issue={issue}
        />
      )}
    </div>
  );
}
