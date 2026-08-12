"use client";

import Link from "next/link";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useSprintsList } from "@/lib/hooks/useSprints";
import { ProjectStatusChip, SprintStatusChip } from "@/components/shell/chips";
import { UserName } from "@/lib/users/directory";
import { formatDateOnly } from "@/lib/format";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shell/EmptyState";
import { Users, LayersIcon } from "lucide-react";

export default function ProjectOverviewPage() {
  const { projectId, project, isLoading, isLeader, fieldDefinitionsByKind } = useProjectContext();
  // "IN_PROGRESS" was a hardcoded literal — SPRINT_STATUS codes flagged
  // isActiveWork are this project's "currently running" set now (docs/API.md §2).
  const runningStatuses = new Set(
    (fieldDefinitionsByKind.get("SPRINT_STATUS") ?? []).filter((d) => d.isActiveWork).map((d) => d.code),
  );
  const { data: sprints } = useSprintsList(projectId, { size: 100, sort: "startDate,desc" });
  const runningSprint = sprints?.content.find((s) => runningStatuses.has(s.status));

  if (isLoading || !project) {
    return (
      <div className="space-y-4 p-6">
        <Skeleton className="h-8 w-72" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  return (
    <div className="max-w-3xl p-6">
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h1 className="font-heading text-xl font-semibold tracking-tight">{project.name}</h1>
          <div className="mt-1.5 flex items-center gap-3 text-sm text-slate">
            <ProjectStatusChip status={project.status} />
            <span>
              {formatDateOnly(project.startDate)} – {project.endDate ? formatDateOnly(project.endDate) : "ongoing"}
            </span>
          </div>
        </div>
      </div>

      {project.description && (
        <p className="mb-6 whitespace-pre-wrap text-sm leading-relaxed text-ink">
          {project.description}
        </p>
      )}

      <div className="mb-6 grid grid-cols-3 gap-3">
        <div className="rounded border border-rule p-3">
          <p className="text-xs text-slate">Leader</p>
          <p className="mt-1 text-sm font-medium">
            {project.leaderId ? <UserName id={project.leaderId} /> : "Unassigned"}
            {isLeader && <span className="ml-1.5 text-xs text-signal">(you)</span>}
          </p>
        </div>
        <Link href={`/projects/${projectId}/settings`} className="rounded border border-rule p-3 hover:border-signal">
          <p className="flex items-center gap-1 text-xs text-slate">
            <Users className="h-3 w-3" /> Participants
          </p>
          <p className="mt-1 font-data text-sm font-medium">{project.memberCount}</p>
        </Link>
        <Link href={`/projects/${projectId}/settings`} className="rounded border border-rule p-3 hover:border-signal">
          <p className="flex items-center gap-1 text-xs text-slate">
            <LayersIcon className="h-3 w-3" /> Teams
          </p>
          <p className="mt-1 font-data text-sm font-medium">{project.teamCount}</p>
        </Link>
      </div>

      <div>
        <h2 className="mb-2 font-heading text-sm font-semibold">Current sprint</h2>
        {runningSprint ? (
          <Link
            href={`/projects/${projectId}/board`}
            className="flex items-center justify-between rounded border border-rule p-3 hover:border-signal"
          >
            <div>
              <p className="text-sm font-medium">{runningSprint.name}</p>
              <p className="mt-0.5 font-data text-xs text-slate">
                {formatDateOnly(runningSprint.startDate)} – {formatDateOnly(runningSprint.endDate)}
              </p>
            </div>
            <SprintStatusChip status={runningSprint.status} />
          </Link>
        ) : (
          <EmptyState
            title="No sprint is running"
            description="Start one from the Sprints tab to see it here and on the board."
          />
        )}
      </div>
    </div>
  );
}
