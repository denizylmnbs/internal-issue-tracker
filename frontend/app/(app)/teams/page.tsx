"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { useTeamsList } from "@/lib/hooks/useTeams";
import { DataTable, type Column } from "@/components/shell/DataTable";
import { UserName } from "@/lib/users/directory";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CreateTeamDialog } from "@/components/pickers/CreateTeamDialog";
import { useSession } from "@/lib/auth/session";
import { isEditorOrAbove } from "@/lib/auth/can";
import type { TeamResponse } from "@/lib/api/types";

const columns: Column<TeamResponse>[] = [
  { key: "name", header: "Name", render: (t) => <span className="font-medium">{t.name}</span> },
  { key: "field", header: "Field", render: (t) => t.field ?? "—" },
  { key: "leader", header: "Leader", render: (t) => <UserName id={t.leaderId} /> },
];

export default function TeamsPage() {
  const router = useRouter();
  const { user } = useSession();
  const [name, setName] = useState("");
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isLoading } = useTeamsList({ name: name || undefined, sort: "name,asc", size: 100 });

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-heading text-xl font-semibold tracking-tight">Teams</h1>
        {isEditorOrAbove(user) && (
          <Button size="sm" onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" />
            New team
          </Button>
        )}
      </div>

      <Input
        placeholder="Filter by name…"
        value={name}
        onChange={(e) => setName(e.target.value)}
        className="mb-4 max-w-64"
      />

      <DataTable
        columns={columns}
        rows={data?.content.filter((t) => t.isActive)}
        rowKey={(t) => t.id}
        isLoading={isLoading}
        emptyTitle="No teams yet"
        emptyDescription="Create a team to start assigning people and projects."
        onRowClick={(t) => router.push(`/teams/${t.id}`)}
      />

      <CreateTeamDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}
