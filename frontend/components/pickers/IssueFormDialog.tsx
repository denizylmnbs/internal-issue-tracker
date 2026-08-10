"use client";

import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { UserPicker } from "./UserPicker";
import { TeamPicker } from "./TeamPicker";
import { SprintPicker, EpicPicker } from "./SprintEpicPickers";
import { useCreateIssue, useUpdateIssue } from "@/lib/hooks/useIssues";
import {
  ISSUE_TYPES,
  ISSUE_TYPE_LABEL,
  ISSUE_PRIORITIES,
  ISSUE_PRIORITY_LABEL,
  ISSUE_RESOLVING_UNITS,
  ISSUE_RESOLVING_UNIT_LABEL,
} from "@/lib/api/enums";
import type { IssueResponse } from "@/lib/api/types";

// type is required at creation, but never a status field — nothing is ever
// created in a chosen status (docs/API.md §2).
const schema = z.object({
  name: z.string().min(2, "At least 2 characters").max(255),
  description: z.string().max(5000).optional(),
  type: z.enum(ISSUE_TYPES),
  priority: z.enum(ISSUE_PRIORITIES),
  resolvingUnit: z.enum(ISSUE_RESOLVING_UNITS).nullable(),
  storyPoint: z.number().min(0).nullable(),
  sprintId: z.number().nullable(),
  epicId: z.number().nullable(),
  assigneeUserId: z.number().nullable(),
  assigneeTeamId: z.number().nullable(),
});
type FormValues = z.infer<typeof schema>;

/** Handles both create and edit. Edit sends every field on save (docs/API.md
 * §5 note 2 — PUT is a full replacement; omitting sprintId would clear it). */
export function IssueFormDialog({
  open,
  onOpenChange,
  projectId,
  issue,
  defaultSprintId,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: number;
  issue?: IssueResponse;
  defaultSprintId?: number | null;
}) {
  const router = useRouter();
  const createIssue = useCreateIssue(projectId);
  const updateIssue = useUpdateIssue(projectId, issue?.id ?? 0);
  const isEdit = !!issue;

  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: issue
      ? {
          name: issue.name,
          description: issue.description ?? "",
          type: issue.type,
          priority: issue.priority,
          resolvingUnit: issue.resolvingUnit,
          storyPoint: issue.storyPoint,
          sprintId: issue.sprintId,
          epicId: issue.epicId,
          assigneeUserId: issue.assigneeUserId,
          assigneeTeamId: issue.assigneeTeamId,
        }
      : {
          name: "",
          description: "",
          type: "TASK",
          priority: "MEDIUM",
          resolvingUnit: null,
          storyPoint: null,
          sprintId: defaultSprintId ?? null,
          epicId: null,
          assigneeUserId: null,
          assigneeTeamId: null,
        },
  });

  const onSubmit = (values: FormValues) => {
    if (isEdit && issue) {
      updateIssue.mutate(
        {
          name: values.name,
          description: values.description || undefined,
          type: values.type,
          priority: values.priority,
          resolvingUnit: values.resolvingUnit ?? undefined,
          storyPoint: values.storyPoint ?? undefined,
          sprintId: values.sprintId ?? undefined,
          epicId: values.epicId ?? undefined,
        },
        {
          onSuccess: () => {
            toast.success("Issue updated.");
            onOpenChange(false);
          },
        },
      );
    } else {
      createIssue.mutate(
        {
          name: values.name,
          description: values.description || undefined,
          type: values.type,
          priority: values.priority,
          resolvingUnit: values.resolvingUnit ?? undefined,
          storyPoint: values.storyPoint ?? undefined,
          sprintId: values.sprintId ?? undefined,
          epicId: values.epicId ?? undefined,
          assigneeUserId: values.assigneeUserId ?? undefined,
          assigneeTeamId: values.assigneeTeamId ?? undefined,
        },
        {
          onSuccess: (created) => {
            toast.success("Issue created.");
            reset();
            onOpenChange(false);
            router.push(`/projects/${projectId}/issues/${created.id}`);
          },
        },
      );
    }
  };

  const pending = createIssue.isPending || updateIssue.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogHeader>
            <DialogTitle>{isEdit ? `Edit ISS-${issue!.id}` : "New issue"}</DialogTitle>
          </DialogHeader>

          <div className="max-h-[65vh] space-y-3 overflow-y-auto py-3 pr-1">
            <div className="space-y-1.5">
              <Label htmlFor="name">Name</Label>
              <Input id="name" {...register("name")} />
              {errors.name && <p className="text-xs text-rust">{errors.name.message}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" rows={3} {...register("description")} />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>Type</Label>
                <Controller
                  control={control}
                  name="type"
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger className="w-full">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {ISSUE_TYPES.map((t) => (
                          <SelectItem key={t} value={t}>
                            {ISSUE_TYPE_LABEL[t]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>
              <div className="space-y-1.5">
                <Label>Priority</Label>
                <Controller
                  control={control}
                  name="priority"
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger className="w-full">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {ISSUE_PRIORITIES.map((p) => (
                          <SelectItem key={p} value={p}>
                            {ISSUE_PRIORITY_LABEL[p]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label>Resolving unit</Label>
              <Controller
                control={control}
                name="resolvingUnit"
                render={({ field }) => (
                  <Select
                    value={field.value ?? "NONE"}
                    onValueChange={(v) => field.onChange(v === "NONE" ? null : v)}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="NONE">Unassigned</SelectItem>
                      {ISSUE_RESOLVING_UNITS.map((u) => (
                        <SelectItem key={u} value={u}>
                          {ISSUE_RESOLVING_UNIT_LABEL[u]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>Sprint</Label>
                <Controller
                  control={control}
                  name="sprintId"
                  render={({ field }) => (
                    <SprintPicker projectId={projectId} value={field.value} onChange={field.onChange} />
                  )}
                />
              </div>
              <div className="space-y-1.5">
                <Label>Epic</Label>
                <Controller
                  control={control}
                  name="epicId"
                  render={({ field }) => (
                    <EpicPicker projectId={projectId} value={field.value} onChange={field.onChange} />
                  )}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="storyPoint">Story points</Label>
              <Controller
                control={control}
                name="storyPoint"
                render={({ field }) => (
                  <Input
                    id="storyPoint"
                    type="number"
                    min={0}
                    value={field.value ?? ""}
                    onChange={(e) => field.onChange(e.target.value === "" ? null : Number(e.target.value))}
                  />
                )}
              />
            </div>

            {!isEdit && (
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label>Assignee</Label>
                  <Controller
                    control={control}
                    name="assigneeUserId"
                    render={({ field }) => (
                      <UserPicker value={field.value} onChange={field.onChange} placeholder="Unassigned" />
                    )}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>Assigned team</Label>
                  <Controller
                    control={control}
                    name="assigneeTeamId"
                    render={({ field }) => (
                      <TeamPicker value={field.value} onChange={field.onChange} />
                    )}
                  />
                </div>
              </div>
            )}
          </div>

          <DialogFooter>
            <Button type="submit" disabled={pending}>
              {pending ? "Saving…" : isEdit ? "Save changes" : "Create issue"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
