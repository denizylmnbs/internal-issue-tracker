"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useSession } from "@/lib/auth/session";
import { getUserProjects, getUserTeams } from "@/lib/api/endpoints/users";
import { ProjectStatusChip } from "@/components/shell/chips";
import { EmptyState } from "@/components/shell/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";

/** docs/API.md §5: "the endpoint to build the logged-in user's 'My projects'
 * home page from" is GET /api/users/{me}/projects, alongside /teams. */
export default function MyWorkPage() {
  const { user } = useSession();

  const { data: projects, isLoading: loadingProjects } = useQuery({
    queryKey: ["users", user?.id, "projects"],
    queryFn: () => getUserProjects(user!.id, { size: 50 }),
    enabled: !!user,
  });

  const { data: teams, isLoading: loadingTeams } = useQuery({
    queryKey: ["users", user?.id, "teams"],
    queryFn: () => getUserTeams(user!.id, { size: 50 }),
    enabled: !!user,
  });

  return (
    <div className="max-w-3xl p-6">
      <h1 className="mb-1 font-heading text-xl font-semibold tracking-tight">
        {user ? `Welcome back, ${user.name}` : "My work"}
      </h1>
      <p className="mb-6 text-sm text-slate">Your projects and teams, in one place.</p>

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
