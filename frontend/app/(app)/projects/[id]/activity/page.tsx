"use client";

import { useProjectContext } from "@/lib/project/ProjectContext";
import { useProjectActivity } from "@/lib/hooks/useActivity";
import { ActivitySpine } from "@/components/spine/ActivitySpine";
import { Skeleton } from "@/components/ui/skeleton";

export default function ProjectActivityPage() {
  const { projectId } = useProjectContext();
  const { data, isLoading } = useProjectActivity(projectId, { size: 100 });

  return (
    <div className="max-w-2xl p-6">
      <h1 className="mb-4 font-heading text-xl font-semibold tracking-tight">Activity</h1>
      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-3/4" />
        </div>
      ) : (
        <ActivitySpine activities={data?.content ?? []} domain="project" projectId={projectId} />
      )}
    </div>
  );
}
