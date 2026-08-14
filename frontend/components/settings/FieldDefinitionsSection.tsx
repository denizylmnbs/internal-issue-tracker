"use client";

import { useState } from "react";
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  verticalListSortingStrategy,
  useSortable,
  arrayMove,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { GripVertical, Plus, Pencil, Trash2 } from "lucide-react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useFieldDefinitionsList,
  useReorderFieldDefinitions,
  useDeleteFieldDefinition,
  useFieldDefinitionUsage,
} from "@/lib/hooks/useFieldDefinitions";
import { FieldDefinitionFormDialog } from "./FieldDefinitionFormDialog";
import { resolveColor } from "@/lib/fielddef/colors";
import type { FieldDefinitionResponse, FieldKind } from "@/lib/api/types";

const KIND_LABEL: Record<FieldKind, string> = {
  PROJECT_STATUS: "Project status",
  SPRINT_STATUS: "Sprint status",
  EPIC_STATUS: "Epic status",
  ISSUE_STATUS: "Issue status",
  ISSUE_TYPE: "Issue type",
  ISSUE_PRIORITY: "Issue priority",
  ISSUE_UNIT: "Resolving unit",
  TEAM_FIELD: "Team field",
};

const FLAG_BADGES: { key: keyof FieldDefinitionResponse; label: string }[] = [
  { key: "isDefault", label: "default" },
  { key: "isDone", label: "done" },
  { key: "isCancelled", label: "cancelled" },
  { key: "isActiveWork", label: "active" },
  { key: "isDefect", label: "defect" },
];

/**
 * The management UI the migration exists for: without this, nobody can
 * actually add a status/type/priority/unit of their own — see docs/API.md
 * §2/§4.14. One tab per kind; each tab is an independently-loaded, -reordered
 * and -edited list, since PATCH .../reorder validates against exactly one
 * kind's active rows at a time.
 */
export function FieldDefinitionsSection({
  projectId,
  kinds,
  canManage,
}: {
  /** null for the two global kinds (rendered from the admin page). */
  projectId: number | null;
  kinds: FieldKind[];
  canManage: boolean;
}) {
  const [activeKind, setActiveKind] = useState<FieldKind>(kinds[0]);

  return (
    <Tabs value={activeKind} onValueChange={(v) => setActiveKind(v as FieldKind)}>
      <TabsList>
        {kinds.map((k) => (
          <TabsTrigger key={k} value={k}>
            {KIND_LABEL[k]}
          </TabsTrigger>
        ))}
      </TabsList>
      {kinds.map((k) => (
        <TabsContent key={k} value={k} className="pt-3">
          <KindList projectId={projectId} kind={k} canManage={canManage} />
        </TabsContent>
      ))}
    </Tabs>
  );
}

function KindList({
  projectId,
  kind,
  canManage,
}: {
  projectId: number | null;
  kind: FieldKind;
  canManage: boolean;
}) {
  const { data, isLoading } = useFieldDefinitionsList(projectId, kind);
  const reorder = useReorderFieldDefinitions(projectId);
  const del = useDeleteFieldDefinition(projectId);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<FieldDefinitionResponse | undefined>();
  const [pendingDelete, setPendingDelete] = useState<FieldDefinitionResponse | undefined>();
  const [reassignTo, setReassignTo] = useState<string | undefined>();
  const usage = useFieldDefinitionUsage(projectId, pendingDelete?.id);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 4 } }));
  const rows = [...(data ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);

  const onDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIndex = rows.findIndex((r) => r.id === active.id);
    const newIndex = rows.findIndex((r) => r.id === over.id);
    if (oldIndex < 0 || newIndex < 0) return;
    const next = arrayMove(rows, oldIndex, newIndex);
    reorder.mutate({ kind, orderedIds: next.map((r) => r.id) });
  };

  const openCreate = () => {
    setEditing(undefined);
    setFormOpen(true);
  };
  const openEdit = (def: FieldDefinitionResponse) => {
    setEditing(def);
    setFormOpen(true);
  };

  if (isLoading) return <Skeleton className="h-32 w-full" />;

  return (
    <div className="space-y-2">
      {canManage && (
        <div className="flex justify-end">
          <Button size="sm" variant="outline" onClick={openCreate}>
            <Plus className="h-3.5 w-3.5" />
            Add
          </Button>
        </div>
      )}

      {rows.length === 0 ? (
        <p className="py-4 text-sm text-slate">No values defined for this kind yet.</p>
      ) : (
        <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
          <SortableContext items={rows.map((r) => r.id)} strategy={verticalListSortingStrategy}>
            <div className="divide-y divide-rule rounded border border-rule">
              {rows.map((def) => (
                <Row
                  key={def.id}
                  def={def}
                  canManage={canManage}
                  onEdit={() => openEdit(def)}
                  onDelete={() => {
                    setReassignTo(undefined);
                    setPendingDelete(def);
                  }}
                />
              ))}
            </div>
          </SortableContext>
        </DndContext>
      )}

      {formOpen && (
        <FieldDefinitionFormDialog
          open={formOpen}
          onOpenChange={setFormOpen}
          projectId={projectId}
          kind={kind}
          definition={editing}
        />
      )}

      <AlertDialog
        open={!!pendingDelete}
        onOpenChange={(v) => {
          if (!v) {
            setPendingDelete(undefined);
            setReassignTo(undefined);
          }
        }}
      >
        <AlertDialogContent>
          {(() => {
            const count = usage.data?.count ?? 0;
            const inUse = !usage.isLoading && count > 0;
            const alternatives = rows.filter((r) => r.id !== pendingDelete?.id);

            return (
              <>
                <AlertDialogHeader>
                  <AlertDialogTitle>Delete &quot;{pendingDelete?.label}&quot;?</AlertDialogTitle>
                  <AlertDialogDescription asChild>
                    <div className="space-y-3">
                      {usage.isLoading ? (
                        <p>Checking whether anything still uses this value…</p>
                      ) : inUse ? (
                        <>
                          <p>
                            {count} record{count === 1 ? "" : "s"} still carr
                            {count === 1 ? "ies" : "y"} this value. Pick a replacement to move{" "}
                            {count === 1 ? "it" : "them"} to before deleting — otherwise{" "}
                            {count === 1 ? "it" : "they"} would silently drop out of any view
                            grouped by this field.
                          </p>
                          {alternatives.length === 0 ? (
                            <p className="font-medium text-ink">
                              There&apos;s no other value of this kind to move them to — add one
                              first.
                            </p>
                          ) : (
                            <Select value={reassignTo} onValueChange={setReassignTo}>
                              <SelectTrigger className="w-full">
                                <SelectValue placeholder="Move to…" />
                              </SelectTrigger>
                              <SelectContent>
                                {alternatives.map((alt) => (
                                  <SelectItem key={alt.code} value={alt.code}>
                                    {alt.label}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          )}
                        </>
                      ) : (
                        <p>
                          Nothing currently uses this value. Blocked if this is the last value
                          flagged &quot;done&quot; or the current default — the server will say
                          which.
                        </p>
                      )}
                    </div>
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    disabled={usage.isLoading || (inUse && !reassignTo)}
                    onClick={() => {
                      if (pendingDelete) {
                        del.mutate({ defId: pendingDelete.id, reassignTo });
                      }
                      setPendingDelete(undefined);
                      setReassignTo(undefined);
                    }}
                  >
                    Delete
                  </AlertDialogAction>
                </AlertDialogFooter>
              </>
            );
          })()}
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

function Row({
  def,
  canManage,
  onEdit,
  onDelete,
}: {
  def: FieldDefinitionResponse;
  canManage: boolean;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: def.id,
    disabled: !canManage,
  });
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <div ref={setNodeRef} style={style} className="flex items-center gap-2 px-3 py-2">
      {canManage && (
        <button
          {...attributes}
          {...listeners}
          className="cursor-grab text-slate hover:text-ink"
          aria-label="Reorder"
        >
          <GripVertical className="h-4 w-4" />
        </button>
      )}
      <span
        className="h-2.5 w-2.5 shrink-0 rounded-full"
        style={{ backgroundColor: resolveColor(def, def.code) }}
      />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium">{def.label}</p>
        <p className="font-data text-xs text-slate">{def.code}</p>
      </div>
      <div className="flex flex-wrap justify-end gap-1">
        {FLAG_BADGES.filter(({ key }) => def[key]).map(({ key, label }) => (
          <Badge key={key} variant="secondary" className="text-[10px]">
            {label}
          </Badge>
        ))}
      </div>
      {canManage && (
        <div className="flex shrink-0 gap-1">
          <Button variant="ghost" size="icon" onClick={onEdit} title="Edit">
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          <Button variant="ghost" size="icon" onClick={onDelete} title="Delete">
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      )}
    </div>
  );
}
