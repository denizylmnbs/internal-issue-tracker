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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { UserPicker } from "./UserPicker";
import { useCreateTeam } from "@/lib/hooks/useTeams";
import { useGlobalFieldDefinitions } from "@/lib/fielddef/GlobalFieldDefinitionsProvider";

const schema = z.object({
  name: z.string().min(2, "At least 2 characters").max(255),
  field: z.string().optional(),
  leaderId: z.number({ error: "A leader is required" }).nullable(),
});
type FormValues = z.infer<typeof schema>;

export function CreateTeamDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const router = useRouter();
  const createTeam = useCreateTeam();
  const { listGlobal } = useGlobalFieldDefinitions();
  const fields = listGlobal("TEAM_FIELD");

  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", field: undefined, leaderId: null },
  });

  const onSubmit = (values: FormValues) => {
    if (values.leaderId == null) return;
    createTeam.mutate(
      {
        name: values.name,
        field: values.field,
        leaderId: values.leaderId,
      },
      {
        onSuccess: (team) => {
          toast.success(`${team.name} created.`);
          reset();
          onOpenChange(false);
          router.push(`/teams/${team.id}`);
        },
      },
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogHeader>
            <DialogTitle>New team</DialogTitle>
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
                      <SelectValue placeholder="Optional" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="NONE">None</SelectItem>
                      {fields.map((f) => (
                        <SelectItem key={f.code} value={f.code}>
                          {f.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Leader</Label>
              <Controller
                control={control}
                name="leaderId"
                render={({ field }) => (
                  <UserPicker value={field.value} onChange={field.onChange} eligibleOnly />
                )}
              />
              {errors.leaderId && <p className="text-xs text-rust">{errors.leaderId.message}</p>}
            </div>
          </div>
          <DialogFooter>
            <Button type="submit" disabled={createTeam.isPending}>
              {createTeam.isPending ? "Creating…" : "Create team"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
