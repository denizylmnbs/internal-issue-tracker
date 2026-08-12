"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useIssuesList, useBulkIssueEdit } from "@/lib/hooks/useIssues";
import { DataTable, type Column, type RowKey } from "@/components/shell/DataTable";
import { PriorityChip, TypeChip } from "@/components/shell/chips";
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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import type { IssueResponse } from "@/lib/api/types";
import { deleteIssue } from "@/lib/api/endpoints/issues";
import { IssueFormDialog } from "@/components/pickers/IssueFormDialog";
import { IssueActionsMenu, IssueContextMenu } from "@/components/backlog/IssueActions";

export default function BacklogPage() {
  const router = useRouter();
  const { projectId, canWork, fieldDefinitionsByKind, defaultCodeFor } = useProjectContext();
  const statuses = fieldDefinitionsByKind.get("ISSUE_STATUS") ?? [];
  const [name, setName] = useState("");
  const [status, setStatus] = useState<string | undefined>(defaultCodeFor("ISSUE_STATUS"));
  const [createOpen, setCreateOpen] = useState(false);
  const [selected, setSelected] = useState<Set<RowKey>>(new Set());
  const [editing, setEditing] = useState<IssueResponse | undefined>();
  const [pendingDelete, setPendingDelete] = useState<IssueResponse[]>([]);

  const { data, isLoading } = useIssuesList(projectId, {
    name: name || undefined,
    status,
    size: 200,
    sort: "priority,desc",
  });

  const bulk = useBulkIssueEdit(projectId);
  const rows = data?.content ?? [];
  const selectedIssues = rows.filter((i) => selected.has(i.id));

  /** A filter change hides rows; acting on a selection you can no longer see
   * is never what was meant. */
  const resetSelection = () => setSelected(new Set());

  /** Right-clicking inside the selection acts on all of it, and right-clicking
   * outside narrows to the one row — what every file list does. */
  const targetsFor = (issue: IssueResponse) =>
    selected.has(issue.id) && selectedIssues.length > 0 ? selectedIssues : [issue];

  const confirmDelete = () => {
    const targets = pendingDelete;
    setPendingDelete([]);
    bulk.mutate({
      issueIds: targets.map((i) => i.id),
      apply: (id) => deleteIssue(projectId, id),
      describe: (n) => `${n === 1 ? "Issue" : `${n} issues`} deleted.`,
    });
    resetSelection();
  };

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
        <Input
          placeholder="Filter by name…"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            resetSelection();
          }}
          className="max-w-64"
        />
        <Select
          value={status}
          onValueChange={(v) => {
            setStatus(v);
            resetSelection();
          }}
        >
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {statuses.map((s) => (
              <SelectItem key={s.code} value={s.code}>
                {s.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {canWork && selectedIssues.length > 0 && (
        <div className="mb-3 flex items-center gap-3 rounded border border-rule bg-muted/40 px-3 py-2">
          <span className="text-sm text-slate">
            {selectedIssues.length} selected
          </span>
          <IssueActionsMenu
            projectId={projectId}
            targets={selectedIssues}
            onEdit={setEditing}
            onDelete={setPendingDelete}
          />
          <Button size="sm" variant="ghost" onClick={resetSelection}>
            Clear
          </Button>
          <span className="ml-auto text-xs text-slate">
            Tip: right-click a row for the same menu.
          </span>
        </div>
      )}

      <DataTable
        columns={columns}
        rows={data?.content}
        rowKey={(i) => i.id}
        isLoading={isLoading}
        emptyTitle="Nothing here"
        emptyDescription="File the first issue to get this list started."
        onRowClick={(i) => router.push(`/projects/${projectId}/issues/${i.id}`)}
        selection={canWork ? { selectedKeys: selected, onChange: setSelected } : undefined}
        rowWrapper={
          canWork
            ? (issue, rowElement) => (
                <IssueContextMenu
                  key={issue.id}
                  projectId={projectId}
                  targets={targetsFor(issue)}
                  onEdit={setEditing}
                  onDelete={setPendingDelete}
                  onOpen={() => {
                    if (!selected.has(issue.id)) setSelected(new Set([issue.id]));
                  }}
                >
                  {rowElement}
                </IssueContextMenu>
              )
            : undefined
        }
      />

      {createOpen && (
        <IssueFormDialog open={createOpen} onOpenChange={setCreateOpen} projectId={projectId} />
      )}

      {editing && (
        <IssueFormDialog
          open
          onOpenChange={(open) => !open && setEditing(undefined)}
          projectId={projectId}
          issue={editing}
        />
      )}

      <AlertDialog
        open={pendingDelete.length > 0}
        onOpenChange={(open) => !open && setPendingDelete([])}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              Delete {pendingDelete.length === 1 ? `ISS-${pendingDelete[0]?.id}` : `${pendingDelete.length} issues`}?
            </AlertDialogTitle>
            <AlertDialogDescription>
              This can&apos;t be undone from here.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={confirmDelete}>Delete</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
