"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Kanban,
  ListTodo,
  Rows3,
  Layers,
  Activity,
  LineChart,
  Settings,
  ChevronLeft,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { Skeleton } from "@/components/ui/skeleton";

export function ProjectRail() {
  const pathname = usePathname();
  const { projectId, project, isLoading } = useProjectContext();
  const base = `/projects/${projectId}`;

  const items = [
    { href: base, label: "Overview", icon: Rows3, exact: true },
    { href: `${base}/board`, label: "Board", icon: Kanban },
    { href: `${base}/backlog`, label: "Backlog", icon: ListTodo },
    { href: `${base}/sprints`, label: "Sprints", icon: Layers },
    { href: `${base}/epics`, label: "Epics", icon: Layers },
    { href: `${base}/activity`, label: "Activity", icon: Activity },
    { href: `${base}/insights`, label: "Insights", icon: LineChart },
    { href: `${base}/settings`, label: "Settings", icon: Settings },
  ];

  return (
    <aside className="flex h-full w-52 shrink-0 flex-col border-r border-rule">
      <div className="border-b border-rule p-3">
        <Link href="/projects" className="mb-2 flex items-center gap-1 text-xs text-slate hover:text-ink">
          <ChevronLeft className="h-3 w-3" />
          All projects
        </Link>
        {isLoading ? (
          <Skeleton className="h-5 w-32" />
        ) : (
          <p className="truncate font-heading text-sm font-semibold" title={project?.name}>
            {project?.name}
          </p>
        )}
      </div>
      <nav className="flex-1 space-y-0.5 p-2">
        {items.map(({ href, label, icon: Icon, exact }) => {
          const active = exact ? pathname === href : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-2.5 rounded px-2.5 py-1.5 text-sm transition-colors",
                active ? "bg-accent font-medium text-signal" : "text-ink hover:bg-secondary",
              )}
            >
              <Icon className="h-4 w-4" strokeWidth={2} />
              {label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
