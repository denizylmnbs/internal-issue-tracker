"use client";

import { useForm, Controller } from "react-hook-form";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useUpdateTeam } from "@/lib/hooks/useTeams";
import { TEAM_FIELDS } from "@/lib/api/enums";
import type { TeamField } from "@/lib/api/enums";
import type { TeamResponse } from "@/lib/api/types";

// The leader is deliberately absent — changing it is its own operation
// (docs/API.md §4.3).
const schema = z.object({
  name: z.string().min(2, "At least 2 characters").max(255),
  field: z.string().optional(),
});
type FormValues = z.infer<typeof schema>;

export function TeamFormDialog({
  open,
  onOpenChange,
  team,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  team: TeamResponse;
}) {
  const updateTeam = useUpdateTeam(team.id);

  const { register, handleSubmit, control, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: { name: team.name, field: team.field ?? undefined },
  });

  const onSubmit = (values: FormValues) => {
    updateTeam.mutate(
      { name: values.name, field: values.field as TeamField | undefined },
      { onSuccess: () => { toast.success("Team updated."); onOpenChange(false); } },
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogHeader>
            <DialogTitle>Edit team</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 py-3">
            <div className="space-y-1.5">
              <Label htmlFor="name">Name</Label>
              <Input id="name" {...register("name")} />
              {errors.name && <p className="text-xs text-rust">{errors.name.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label>Field</Label>
              <Controller
                control={control}
                name="field"
                render={({ field }) => (
                  <Select value={field.value ?? "NONE"} onValueChange={(v) => field.onChange(v === "NONE" ? undefined : v)}>
                    <SelectTrigger className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="NONE">None</SelectItem>
                      {TEAM_FIELDS.map((f) => (
                        <SelectItem key={f} value={f}>{f}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
          </div>
          <DialogFooter>
            <Button type="submit" disabled={updateTeam.isPending}>Save changes</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
