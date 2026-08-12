"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useSession } from "@/lib/auth/session";
import {
  getUserActiveIssues,
  getUserProjects,
  getUserSprintProgress,
  getUserTeams,
} from "@/lib/api/endpoints/users";
import { IssueStatusChip, ProjectStatusChip } from "@/components/shell/chips";
import { EmptyState } from "@/components/shell/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";
import type { SprintProgressEntry, UserProjectMembershipResponse } from "@/lib/api/types";
import { cn } from "@/lib/utils";

function sumPoints(entries: SprintProgressEntry[], field: "assignedPoints" | "completedPoints") {
  return entries.reduce((total, entry) => total + entry[field], 0);
}

function ProgressBar({ completed, assigned }: { completed: number; assigned: number }) {
  const pct = assigned > 0 ? Math.min(100, Math.round((completed / assigned) * 100)) : 0;
  return (
    <div className="h-1.5 w-full overflow-hidden rounded-full bg-secondary">
      <div className="h-full rounded-full bg-signal" style={{ width: `${pct}%` }} />
    </div>
  );
}

function SprintStatTile({
  label,
  entries,
  projectNameById,
}: {
  label: string;
  entries: SprintProgressEntry[];
  projectNameById: Map<number, string>;
}) {
  const assigned = sumPoints(entries, "assignedPoints");
  const completed = sumPoints(entries, "completedPoints");

  return (
    <div className="rounded border border-rule p-3">
      <p className="text-xs text-slate">{label}</p>
      {entries.length === 0 ? (
        <p className="mt-1 text-lg font-semibold text-slate">—</p>
      ) : (
        <>
          <p className="mt-1 text-lg font-semibold">
            {completed} <span className="text-sm font-normal text-slate">/ {assigned} SP</span>
          </p>
          <div className="mt-2">
            <ProgressBar completed={completed} assigned={assigned} />
          </div>
          {entries.length > 1 && (
            <ul className="mt-2 space-y-0.5">
              {entries.map((entry) => (
                <li
                  key={entry.sprintId}
                  className="flex items-center justify-between text-xs text-slate"
                >
                  <span>
                    {projectNameById.get(entry.projectId) ?? `Project ${entry.projectId}`} ·{" "}
                    {entry.sprintName}
                  </span>
                  <span>
                    {entry.completedPoints}/{entry.assignedPoints}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}

/** docs/API.md §5: "the endpoint to build the logged-in user's 'My projects'
 * home page from" is GET /api/users/{me}/projects, alongside /teams. */
export default function MyWorkPage() {
  const { user } = useSession();

  const { data: projects, isLoading: loadingProjects } = useQuery({
    queryKey: ["users", user?.id, "projects"],
    queryFn: () => getUserProjects(user!.id, { size: 50 }),
    enabled: !!user,
  });

  const {
    data: teams,
    isLoading: loadingTeams,
    isError: teamsError,
  } = useQuery({
    queryKey: ["users", user?.id, "teams"],
    queryFn: () => getUserTeams(user!.id, { size: 50 }),
    enabled: !!user,
  });

  const { data: activeIssues, isLoading: loadingIssues } = useQuery({
    queryKey: ["users", user?.id, "issues"],
    queryFn: () => getUserActiveIssues(user!.id, { size: 20 }),
    enabled: !!user,
  });

  const {
    data: sprintProgress,
    isLoading: loadingProgress,
    isError: progressError,
  } = useQuery({
    queryKey: ["users", user?.id, "sprint-progress"],
    queryFn: () => getUserSprintProgress(user!.id),
    enabled: !!user,
  });

  const projectNameById = new Map<number, string>(
    (projects?.content ?? []).map((p: UserProjectMembershipResponse) => [p.projectId, p.projectName]),
  );

  return (
    <div className="max-w-3xl p-6">
      <h1 className="mb-1 font-heading text-xl font-semibold tracking-tight">
        {user ? `Welcome back, ${user.name}` : "My work"}
      </h1>
      <p className="mb-6 text-sm text-slate">Your projects and teams, in one place.</p>

      <section className="mb-8">
        <h2 className="mb-2 font-heading text-sm font-semibold">Sprint progress</h2>
        {loadingProgress ? (
          <Skeleton className="h-24 w-full" />
        ) : progressError ? (
          <p className="text-sm text-rust">Could not load sprint progress. Try refreshing.</p>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <SprintStatTile
              label="This sprint"
              entries={sprintProgress?.current ?? []}
              projectNameById={projectNameById}
            />
            <SprintStatTile
              label="Last sprint"
              entries={sprintProgress?.previous ?? []}
              projectNameById={projectNameById}
            />
            <div className="rounded border border-rule p-3">
              <p className="text-xs text-slate">
                Average
                {sprintProgress && sprintProgress.recentSprintCount > 0
                  ? ` (last ${sprintProgress.recentSprintCount} sprint${sprintProgress.recentSprintCount === 1 ? "" : "s"})`
                  : ""}
              </p>
              <p className="mt-1 text-lg font-semibold">
                {sprintProgress?.recentAveragePoints != null
                  ? `${sprintProgress.recentAveragePoints.toFixed(1)} SP`
                  : "—"}
              </p>
            </div>
          </div>
        )}
      </section>

      <section className="mb-8">
        <h2 className="mb-2 font-heading text-sm font-semibold">Active issues</h2>
        {loadingIssues ? (
          <Skeleton className="h-24 w-full" />
        ) : !activeIssues?.content.length ? (
          <EmptyState
            title="Nothing active"
            description="Issues assigned to you that are To do, In progress or In review show up here."
          />
        ) : (
          <div className="divide-y divide-rule rounded border border-rule">
            {activeIssues.content.map((issue) => (
              <Link
                key={issue.id}
                href={`/projects/${issue.projectId}/issues/${issue.id}`}
                className="flex items-center justify-between gap-3 px-3 py-2.5 hover:bg-secondary"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <IssueStatusChip status={issue.status} />
                  <span className="truncate text-sm font-medium">{issue.name}</span>
                </div>
                <div className="flex shrink-0 items-center gap-3 text-xs text-slate">
                  <span>{projectNameById.get(issue.projectId) ?? `Project ${issue.projectId}`}</span>
                  <span className={cn(issue.storyPoint == null && "text-slate/60")}>
                    {issue.storyPoint ?? "—"} SP
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      <section className="mb-8">
        <h2 className="mb-2 font-heading text-sm font-semibold">Projects</h2>
        {loadingProjects ? (
          <Skeleton className="h-24 w-full" />
        ) : !projects?.content.length ? (
          <EmptyState
            title="No projects yet"
            description="Once you're added to a project — directly or through a team — it shows up here."
            action={
              <Link href="/projects" className="text-sm text-signal hover:underline">
                Browse all projects →
              </Link>
            }
          />
        ) : (
          <div className="divide-y divide-rule rounded border border-rule">
            {projects.content.map((p) => (
              <Link
                key={p.projectId}
                href={`/projects/${p.projectId}`}
                className="flex items-center justify-between px-3 py-2.5 hover:bg-secondary"
              >
                <span className="text-sm font-medium">{p.projectName}</span>
                <div className="flex items-center gap-3">
                  {!p.directlyAssigned && (
                    <span className="text-xs text-slate">via team</span>
                  )}
                  <ProjectStatusChip status={p.projectStatus} />
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="mb-2 font-heading text-sm font-semibold">Teams</h2>
        {loadingTeams ? (
          <Skeleton className="h-16 w-full" />
        ) : teamsError ? (
          <p className="text-sm text-rust">Could not load teams. Try refreshing.</p>
        ) : !teams?.content.length ? (
          <EmptyState title="Not on a team yet" description="Ask an Editor or a team leader to add you." />
        ) : (
          <div className="divide-y divide-rule rounded border border-rule">
            {teams.content.map((t) => (
              <Link
                key={t.membershipId}
                href={`/teams/${t.teamId}`}
                className="flex items-center justify-between px-3 py-2.5 hover:bg-secondary"
              >
                <span className="text-sm font-medium">{t.teamName}</span>
                <span className="text-xs text-slate">{t.teamField ?? "—"}</span>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
