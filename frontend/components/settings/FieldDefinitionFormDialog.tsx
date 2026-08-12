"use client";

import { useForm, Controller, type Control } from "react-hook-form";
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
import { Switch } from "@/components/ui/switch";
import {
  useCreateFieldDefinition,
  useUpdateFieldDefinition,
} from "@/lib/hooks/useFieldDefinitions";
import type { FieldDefinitionResponse, FieldKind } from "@/lib/api/types";

// Same shape as FieldDefinitionCreateRequest.code (docs/API.md §4.14) — the
// backend rejects anything else with 400 VALIDATION_FAILED.
const CODE_PATTERN = /^[A-Z][A-Z0-9_]*$/;
const COLOR_PATTERN = /^#[0-9A-Fa-f]{6}$/;

const schema = z.object({
  code: z
    .string()
    .min(1, "Required")
    .max(30)
    .regex(CODE_PATTERN, "UPPER_SNAKE_CASE only, starting with a letter"),
  label: z.string().min(1, "Required").max(100),
  color: z.union([z.string().regex(COLOR_PATTERN, "e.g. #22C55E"), z.literal("")]),
  isDefault: z.boolean(),
  isDone: z.boolean(),
  isCancelled: z.boolean(),
  isActiveWork: z.boolean(),
  isDefect: z.boolean(),
});
type FormValues = z.infer<typeof schema>;
type FlagName = "isDefault" | "isDone" | "isCancelled" | "isActiveWork" | "isDefect";

function FlagToggle({
  control,
  name,
  label,
}: {
  control: Control<FormValues>;
  name: FlagName;
  label: string;
}) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field }) => (
        <label className="flex items-center gap-2 text-sm">
          <Switch checked={field.value} onCheckedChange={field.onChange} size="sm" />
          {label}
        </label>
      )}
    />
  );
}

/** Create or edit one field definition. `code` is immutable once created —
 * see FieldDefinition's own javadoc on the backend — so it's disabled on
 * edit rather than merely omitted, to make that visible rather than assumed. */
export function FieldDefinitionFormDialog({
  open,
  onOpenChange,
  projectId,
  kind,
  definition,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** null targets the two global kinds. */
  projectId: number | null;
  kind: FieldKind;
  definition?: FieldDefinitionResponse;
}) {
  const isEdit = !!definition;
  const create = useCreateFieldDefinition(projectId);
  const update = useUpdateFieldDefinition(projectId);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: definition
      ? {
          code: definition.code,
          label: definition.label,
          color: definition.color ?? "",
          isDefault: definition.isDefault,
          isDone: definition.isDone,
          isCancelled: definition.isCancelled,
          isActiveWork: definition.isActiveWork,
          isDefect: definition.isDefect,
        }
      : {
          code: "",
          label: "",
          color: "",
          isDefault: false,
          isDone: false,
          isCancelled: false,
          isActiveWork: false,
          isDefect: false,
        },
  });

  const onSubmit = (values: FormValues) => {
    const flags = {
      isDefault: values.isDefault,
      isDone: values.isDone,
      isCancelled: values.isCancelled,
      isActiveWork: values.isActiveWork,
      isDefect: values.isDefect,
    };

    if (isEdit && definition) {
      update.mutate(
        {
          defId: definition.id,
          body: { label: values.label, color: values.color || undefined, ...flags },
        },
        {
          onSuccess: () => {
            toast.success("Updated.");
            onOpenChange(false);
          },
        },
      );
    } else {
      create.mutate(
        { kind, code: values.code, label: values.label, color: values.color || undefined, ...flags },
        {
          onSuccess: () => {
            toast.success("Created.");
            onOpenChange(false);
          },
        },
      );
    }
  };

  const pending = create.isPending || update.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogHeader>
            <DialogTitle>{isEdit ? "Edit value" : "New value"}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 py-3">
            <div className="space-y-1.5">
              <Label htmlFor="code">Code</Label>
              <Input id="code" {...register("code")} disabled={isEdit} placeholder="SHIPPED" />
              {errors.code && <p className="text-xs text-rust">{errors.code.message}</p>}
              {isEdit && <p className="text-xs text-slate">Can&apos;t be changed once created.</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="label">Label</Label>
              <Input id="label" {...register("label")} />
              {errors.label && <p className="text-xs text-rust">{errors.label.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="color">Color (optional)</Label>
              <Input id="color" {...register("color")} placeholder="#22C55E" />
              {errors.color && <p className="text-xs text-rust">{errors.color.message}</p>}
              <p className="text-xs text-slate">Left blank, a stable fallback color is used.</p>
            </div>
            <div className="grid grid-cols-2 gap-y-2">
              <FlagToggle control={control} name="isDefault" label="Default" />
              <FlagToggle control={control} name="isDone" label="Done" />
              <FlagToggle control={control} name="isCancelled" label="Cancelled" />
              <FlagToggle control={control} name="isActiveWork" label="Active work" />
              <FlagToggle control={control} name="isDefect" label="Defect (type only)" />
            </div>
          </div>
          <DialogFooter>
            <Button type="submit" disabled={pending}>
              {pending ? "Saving…" : isEdit ? "Save changes" : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
