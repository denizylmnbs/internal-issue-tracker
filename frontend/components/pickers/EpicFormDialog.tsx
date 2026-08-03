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
import { useCreateEpic, useUpdateEpic } from "@/lib/hooks/useEpics";
import type { EpicResponse } from "@/lib/api/types";

const schema = z.object({
  name: z.string().min(2, "At least 2 characters").max(255),
  description: z.string().max(2000).optional(),
});
type FormValues = z.infer<typeof schema>;

export function EpicFormDialog({
  open,
  onOpenChange,
  projectId,
  epic,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: number;
  epic?: EpicResponse;
}) {
  const isEdit = !!epic;
  const createEpic = useCreateEpic(projectId);
  const updateEpic = useUpdateEpic(projectId, epic?.id ?? 0);

  const { register, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: epic
      ? { name: epic.name, description: epic.description ?? "" }
      : { name: "", description: "" },
  });

  const onSubmit = (values: FormValues) => {
    const body = { name: values.name, description: values.description || undefined };
    const mutation = isEdit ? updateEpic : createEpic;
    mutation.mutate(body, {
      onSuccess: () => {
        toast.success(isEdit ? "Epic updated." : "Epic created.");
        onOpenChange(false);
      },
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogHeader>
            <DialogTitle>{isEdit ? "Edit epic" : "New epic"}</DialogTitle>
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
          </div>
          <DialogFooter>
            <Button type="submit" disabled={createEpic.isPending || updateEpic.isPending}>
              {isEdit ? "Save changes" : "Create epic"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
