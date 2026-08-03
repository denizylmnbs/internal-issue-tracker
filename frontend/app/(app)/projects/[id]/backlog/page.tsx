"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useIssuesList } from "@/lib/hooks/useIssues";
import { DataTable, type Column } from "@/components/shell/DataTable";
import { IssueStatusChip, PriorityChip, TypeChip } from "@/components/shell/chips";
import { UserName } from "@/lib/users/directory";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ISSUE_STATUSES, ISSUE_STATUS_LABEL } from "@/lib/api/enums";
import type { IssueStatus } from "@/lib/api/enums";
import type { IssueResponse } from "@/lib/api/types";
import { IssueFormDialog } from "@/components/pickers/IssueFormDialog";

export default function BacklogPage() {
  const router = useRouter();
  const { projectId, canWork } = useProjectContext();
  const [name, setName] = useState("");
  const [status, setStatus] = useState<IssueStatus>("BACKLOG");
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isLoading } = useIssuesList(projectId, {
    name: name || undefined,
    status,
    size: 200,
    sort: "priority,desc",
  });

  const columns: Column<IssueResponse>[] = [
    { key: "id", header: "", render: (i) => <span className="font-data text-xs text-slate">ISS-{i.id}</span> },
    { key: "name", header: "Name", render: (i) => <span className="font-medium">{i.name}</span> },
    { key: "type", header: "Type", render: (i) => <TypeChip type={i.type} /> },
    { key: "priority", header: "Priority", render: (i) => <PriorityChip priority={i.priority} /> },
    { key: "points", header: "Pts", render: (i) => <span className="font-data">{i.storyPoint ?? "—"}</span> },
    { key: "assignee", header: "Assignee", render: (i) => <UserName id={i.assigneeUserId} /> },
  ];

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-heading text-xl font-semibold tracking-tight">Backlog</h1>
        {canWork && (
          <Button size="sm" onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" />
            New issue
          </Button>
        )}
      </div>

      <div className="mb-4 flex gap-2">
        <Input placeholder="Filter by name…" value={name} onChange={(e) => setName(e.target.value)} className="max-w-64" />
        <Select value={status} onValueChange={(v) => setStatus(v as IssueStatus)}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {ISSUE_STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {ISSUE_STATUS_LABEL[s]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <DataTable
        columns={columns}
        rows={data?.content}
        rowKey={(i) => i.id}
        isLoading={isLoading}
        emptyTitle="Nothing here"
        emptyDescription="File the first issue to get this list started."
        onRowClick={(i) => router.push(`/projects/${projectId}/issues/${i.id}`)}
      />

      {createOpen && (
        <IssueFormDialog open={createOpen} onOpenChange={setCreateOpen} projectId={projectId} />
      )}
    </div>
  );
}
