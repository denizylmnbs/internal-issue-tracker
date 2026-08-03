"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
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
import { useCreateSprint, useUpdateSprint } from "@/lib/hooks/useSprints";
import type { SprintResponse } from "@/lib/api/types";

// Status is absent by design — starting a sprint straight from creation would
// slip past the one-running-sprint check (docs/API.md §4.8).
const schema = z
  .object({
    name: z.string().min(2, "At least 2 characters").max(255),
    description: z.string().max(2000).optional(),
    startDate: z.string().min(1, "Start date is required"),
    endDate: z.string().min(1, "End date is required"),
  })
  .refine((v) => v.endDate >= v.startDate, {
    message: "End date can't be before the start date.",
    path: ["endDate"],
  });
type FormValues = z.infer<typeof schema>;

export function SprintFormDialog({
  open,
  onOpenChange,
  projectId,
  sprint,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: number;
  sprint?: SprintResponse;
}) {
  const isEdit = !!sprint;
  const createSprint = useCreateSprint(projectId);
  const updateSprint = useUpdateSprint(projectId, sprint?.id ?? 0);

  const { register, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: sprint
      ? {
          name: sprint.name,
          description: sprint.description ?? "",
          startDate: sprint.startDate,
          endDate: sprint.endDate,
        }
      : { name: "", description: "", startDate: "", endDate: "" },
  });

  const onSubmit = (values: FormValues) => {
    const body = {
      name: values.name,
      description: values.description || undefined,
      startDate: values.startDate,
      endDate: values.endDate,
    };
    const mutation = isEdit ? updateSprint : createSprint;
    mutation.mutate(body, {
      onSuccess: () => {
        toast.success(isEdit ? "Sprint updated." : "Sprint created.");
        onOpenChange(false);
      },
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogHeader>
            <DialogTitle>{isEdit ? "Edit sprint" : "New sprint"}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 py-3">
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
                <Label htmlFor="startDate">Start date</Label>
                <Input id="startDate" type="date" {...register("startDate")} />
                {errors.startDate && <p className="text-xs text-rust">{errors.startDate.message}</p>}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="endDate">End date</Label>
                <Input id="endDate" type="date" {...register("endDate")} />
                {errors.endDate && <p className="text-xs text-rust">{errors.endDate.message}</p>}
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button type="submit" disabled={createSprint.isPending || updateSprint.isPending}>
              {isEdit ? "Save changes" : "Create sprint"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
