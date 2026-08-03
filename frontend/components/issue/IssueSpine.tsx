"use client";

import { isSameDay } from "date-fns";
import { useComments } from "@/lib/hooks/useComments";
import { useIssueActivity } from "@/lib/hooks/useActivity";
import { CommentRow } from "./CommentRow";
import { ActivityRow } from "@/components/spine/ActivityRow";
import { CommentComposer } from "./CommentComposer";
import { EmptyState } from "@/components/shell/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";

type SpineItem =
  | { kind: "comment"; createdAt: string; data: import("@/lib/api/types").CommentResponse }
  | { kind: "activity"; createdAt: string; data: import("@/lib/api/types").ActivityResponse };

/**
 * The signature element: comments and activity interleaved into one
 * chronology, sorted by createdAt — this is the "history is the page"
 * decision from the design plan, not history buried behind a tab.
 */
export function IssueSpine({ projectId, issueId }: { projectId: number; issueId: number }) {
  const { data: comments, isLoading: loadingComments } = useComments(projectId, issueId);
  const { data: activities, isLoading: loadingActivity } = useIssueActivity(projectId, issueId);

  if (loadingComments || loadingActivity) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-3/4" />
      </div>
    );
  }

  const items: SpineItem[] = [
    ...(comments?.content ?? []).map((c) => ({ kind: "comment" as const, createdAt: c.createdAt, data: c })),
    ...(activities?.content ?? []).map((a) => ({ kind: "activity" as const, createdAt: a.createdAt, data: a })),
  ].sort((a, b) => a.createdAt.localeCompare(b.createdAt));

  return (
    <div>
      <CommentComposer projectId={projectId} issueId={issueId} />

      {items.length === 0 ? (
        <EmptyState title="Quiet so far" description="Comments and changes will show up here as they happen." />
      ) : (
        <div>
          {items.map((item, i) => {
            const prev = items[i - 1];
            const showDate = !prev || !isSameDay(new Date(prev.createdAt), new Date(item.createdAt));
            return item.kind === "comment" ? (
              <CommentRow
                key={`c${item.data.id}`}
                comment={item.data}
                projectId={projectId}
                issueId={issueId}
                showDate={showDate}
              />
            ) : (
              <ActivityRow
                key={`a${item.data.id}`}
                activity={item.data}
                domain="issue"
                projectId={projectId}
                showDate={showDate}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}
