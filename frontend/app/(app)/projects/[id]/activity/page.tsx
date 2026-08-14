"use client";

import { useState } from "react";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useProjectActivity } from "@/lib/hooks/useActivity";
import { ActivitySpine } from "@/components/spine/ActivitySpine";
import { Pagination } from "@/components/shell/Pagination";
import { Skeleton } from "@/components/ui/skeleton";

/** One screenful. The feed used to ask for 100 rows and render every one of
 * them in a single scroll, which on a busy project is a long page that still
 * silently cut off at the hundredth event. */
const PAGE_SIZE = 25;

export default function ProjectActivityPage() {
  const { projectId } = useProjectContext();
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, isFetching } = useProjectActivity(projectId, {
    page,
    size: PAGE_SIZE,
  });

  const pageInfo = data?.page;

  return (
    <div className="max-w-2xl p-6">
      <h1 className="mb-4 font-heading text-xl font-semibold tracking-tight">Activity</h1>
      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-3/4" />
        </div>
      ) : isError ? (
        <p className="text-sm text-rust">Could not load activity. Try refreshing.</p>
      ) : (
        <>
          {/* placeholderData keeps the previous page on screen while the next
              one loads, so paging dims rather than collapsing to a skeleton */}
          <div className={isFetching ? "opacity-60 transition-opacity" : undefined}>
            <ActivitySpine activities={data?.content ?? []} projectId={projectId} />
          </div>
          <Pagination
            page={pageInfo?.number ?? page}
            pageCount={pageInfo?.totalPages ?? 0}
            onChange={setPage}
            total={`${pageInfo?.totalElements ?? 0} events`}
            className="mt-4 flex items-center justify-between border-t border-rule pt-3"
          />
        </>
      )}
    </div>
  );
}
