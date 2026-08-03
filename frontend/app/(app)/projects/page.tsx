"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { useProjectsList } from "@/lib/hooks/useProjects";
import { DataTable, type Column } from "@/components/shell/DataTable";
import { ProjectStatusChip } from "@/components/shell/chips";
import { UserName } from "@/lib/users/directory";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PROJECT_STATUSES, PROJECT_STATUS_LABEL } from "@/lib/api/enums";
import type { ProjectStatus } from "@/lib/api/enums";
import type { ProjectResponse } from "@/lib/api/types";
import { formatDateOnly } from "@/lib/format";
import { CreateProjectDialog } from "@/components/pickers/CreateProjectDialog";
import { useSession } from "@/lib/auth/session";
import { isEditorOrAbove } from "@/lib/auth/can";

const columns: Column<ProjectResponse>[] = [
  { key: "name", header: "Name", render: (p) => <span className="font-medium">{p.name}</span> },
  { key: "status", header: "Status", render: (p) => <ProjectStatusChip status={p.status} /> },
  {
    key: "leader",
    header: "Leader",
    render: (p) => (p.leaderId ? <UserName id={p.leaderId} /> : <span className="text-slate">Unassigned</span>),
  },
  { key: "start", header: "Start", render: (p) => <span className="font-data">{formatDateOnly(p.startDate)}</span> },
  {
    key: "end",
    header: "End",
    render: (p) => <span className="font-data">{p.endDate ? formatDateOnly(p.endDate) : "—"}</span>,
  },
];

export default function ProjectsPage() {
  const router = useRouter();
  const { user } = useSession();
  const [name, setName] = useState("");
  const [status, setStatus] = useState<ProjectStatus | "ALL">("ALL");
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isLoading } = useProjectsList({
    name: name || undefined,
    status: status === "ALL" ? undefined : status,
    sort: "name,asc",
    size: 100,
  });

  return (
    <div className="p-6">
      <div className="mb-5 flex items-center justify-between">
        <h1 className="font-heading text-xl font-semibold tracking-tight">Projects</h1>
        {isEditorOrAbove(user) && (
          <Button size="sm" onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" />
            New project
          </Button>
        )}
      </div>

      <div className="mb-4 flex gap-2">
        <Input
          placeholder="Filter by name…"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="max-w-64"
        />
        <Select value={status} onValueChange={(v) => setStatus(v as ProjectStatus | "ALL")}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All statuses</SelectItem>
            {PROJECT_STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {PROJECT_STATUS_LABEL[s]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <DataTable
        columns={columns}
        rows={data?.content}
        rowKey={(p) => p.id}
        isLoading={isLoading}
        emptyTitle="No projects yet"
        emptyDescription="Once a project is opened, it shows up here."
        onRowClick={(p) => router.push(`/projects/${p.id}`)}
      />

      <CreateProjectDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}
