"use client";

import { ProjectProvider } from "@/lib/project/ProjectContext";
import { ProjectRail } from "@/components/shell/ProjectRail";

export function ProjectLayoutClient({
  projectId,
  children,
}: {
  projectId: number;
  children: React.ReactNode;
}) {
  return (
    <ProjectProvider projectId={projectId}>
      <div className="flex h-full">
        <ProjectRail />
        <div className="min-w-0 flex-1 overflow-y-auto">{children}</div>
      </div>
    </ProjectProvider>
  );
}
