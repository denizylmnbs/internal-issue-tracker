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
import { useRailCollapsed } from "@/lib/hooks/useRailCollapsed";
import { Skeleton } from "@/components/ui/skeleton";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { RailLink, RailToggle } from "./rail";

export function ProjectRail() {
  const pathname = usePathname();
  const { projectId, project, isLoading } = useProjectContext();
  const [collapsed, toggle] = useRailCollapsed("project");
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
    <aside
      className={cn(
        "flex h-full shrink-0 flex-col border-r border-rule transition-[width] duration-150",
        collapsed ? "w-14" : "w-52",
      )}
    >
      {collapsed ? (
        <div className="flex flex-col items-center gap-1 border-b border-rule py-2">
          <Tooltip>
            <TooltipTrigger asChild>
              <Link
                href="/projects"
                aria-label="All projects"
                className="flex h-7 w-7 items-center justify-center rounded text-slate hover:bg-secondary hover:text-ink"
              >
                <ChevronLeft className="h-4 w-4" />
              </Link>
            </TooltipTrigger>
            <TooltipContent side="right">All projects</TooltipContent>
          </Tooltip>
          <RailToggle collapsed={collapsed} onToggle={toggle} what="project menu" />
        </div>
      ) : (
        <div className="border-b border-rule p-3">
          <div className="mb-2 flex items-center justify-between gap-1">
            <Link
              href="/projects"
              className="flex min-w-0 items-center gap-1 text-xs text-slate hover:text-ink"
            >
              <ChevronLeft className="h-3 w-3 shrink-0" />
              All projects
            </Link>
            <RailToggle collapsed={collapsed} onToggle={toggle} what="project menu" />
          </div>
          {isLoading ? (
            <Skeleton className="h-5 w-32" />
          ) : (
            <p className="truncate font-heading text-sm font-semibold" title={project?.name}>
              {project?.name}
            </p>
          )}
        </div>
      )}
      <nav className="flex-1 space-y-0.5 p-2">
        {items.map(({ href, label, icon, exact }) => (
          <RailLink
            key={href}
            href={href}
            label={label}
            icon={icon}
            collapsed={collapsed}
            active={exact ? pathname === href : pathname.startsWith(href)}
          />
        ))}
      </nav>
    </aside>
  );
}
