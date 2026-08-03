import Link from "next/link";
import { ChartCard } from "./ChartCard";
import { IssueStatusChip, PriorityChip, TypeChip } from "@/components/shell/chips";
import { formatDurationSeconds } from "@/lib/format";
import type { WipResponse } from "@/lib/api/types";

/** The one metric that reports the present rather than a flow (docs/API.md
 * §4.13). byStatus is the trend a team watches; oldest is the short list it
 * acts on — the only place in these metrics an individual issue is named. */
export function WipPanel({
  projectId,
  data,
  isLoading,
}: {
  projectId: number;
  data: WipResponse | undefined;
  isLoading: boolean;
}) {
  return (
    <ChartCard title="Work in progress" subtitle="right now">
      {isLoading || !data ? (
        <div className="h-32 animate-pulse rounded bg-secondary" />
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {data.byStatus.map((s) => (
              <div key={s.status} className="rounded border border-rule p-2">
                <IssueStatusChip status={s.status} />
                <p className="mt-1.5 font-data text-lg font-semibold leading-none">{s.issueCount}</p>
                <p className="mt-0.5 text-[11px] text-slate">
                  {s.storyPoints}pt · oldest {formatDurationSeconds(s.oldestAgeSeconds)}
                </p>
              </div>
            ))}
          </div>

          {data.oldest.length > 0 && (
            <div>
              <p className="mb-1.5 text-xs font-medium text-slate">Oldest still open</p>
              <div className="divide-y divide-rule rounded border border-rule">
                {data.oldest.slice(0, 8).map((i) => (
                  <Link
                    key={i.issueId}
                    href={`/projects/${projectId}/issues/${i.issueId}`}
                    className="flex items-center justify-between gap-3 px-2.5 py-1.5 hover:bg-secondary"
                  >
                    <span className="font-data text-xs text-slate">ISS-{i.issueId}</span>
                    <TypeChip type={i.type} />
                    <PriorityChip priority={i.priority} />
                    <IssueStatusChip status={i.status} />
                    <span className="ml-auto font-data text-xs text-slate">
                      {formatDurationSeconds(i.ageSeconds)}
                    </span>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </ChartCard>
  );
}
