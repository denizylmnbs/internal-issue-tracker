"use client";

import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useRouter } from "next/navigation";
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
import { UserPicker } from "./UserPicker";
import { useCreateProject } from "@/lib/hooks/useProjects";

// Status is never on a create form (docs/API.md §2 — nothing is created in a
// chosen status; every project starts PLANNING).
const schema = z
  .object({
    name: z.string().min(2, "At least 2 characters").max(255),
    description: z.string().max(2000).optional(),
    startDate: z.string().min(1, "Start date is required"),
    endDate: z.string().optional(),
    leaderId: z.number().nullable(),
  })
  .refine((v) => !v.endDate || v.endDate >= v.startDate, {
    message: "End date can't be before the start date.",
    path: ["endDate"],
  });
type FormValues = z.infer<typeof schema>;

export function CreateProjectDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const router = useRouter();
  const createProject = useCreateProject();

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", description: "", startDate: "", endDate: "", leaderId: null },
  });

  const onSubmit = (values: FormValues) => {
    createProject.mutate(
      {
        name: values.name,
        description: values.description || undefined,
        startDate: values.startDate,
        endDate: values.endDate || undefined,
        leaderId: values.leaderId ?? undefined,
      },
      {
        onSuccess: (project) => {
          toast.success(`${project.name} created.`);
          reset();
          onOpenChange(false);
          router.push(`/projects/${project.id}`);
        },
      },
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogHeader>
            <DialogTitle>New project</DialogTitle>
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
                {errors.startDate && (
                  <p className="text-xs text-rust">{errors.startDate.message}</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="endDate">End date</Label>
                <Input id="endDate" type="date" {...register("endDate")} />
                {errors.endDate && <p className="text-xs text-rust">{errors.endDate.message}</p>}
              </div>
            </div>

            <div className="space-y-1.5">
              <Label>Leader (optional — staff it later if you prefer)</Label>
              <Controller
                control={control}
                name="leaderId"
                render={({ field }) => (
                  <UserPicker value={field.value} onChange={field.onChange} eligibleOnly />
                )}
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="submit" disabled={createProject.isPending}>
              {createProject.isPending ? "Creating…" : "Create project"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
