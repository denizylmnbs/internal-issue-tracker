"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useChangeSprintStatus } from "@/lib/hooks/useSprints";
import { useProjectContext } from "@/lib/project/ProjectContext";

/** A project may have only one running sprint at a time — attempting a
 * second surfaces 409 SPRINT_ALREADY_IN_PROGRESS as a toast via the shared
 * useApiMutation error handling (docs/API.md §5 note 6). */
export function SprintStatusSelect({
  projectId,
  sprintId,
  status,
  disabled,
}: {
  projectId: number;
  sprintId: number;
  status: string;
  disabled?: boolean;
}) {
  const changeStatus = useChangeSprintStatus(projectId, sprintId);
  const { fieldDefinitionsByKind } = useProjectContext();
  const statuses = fieldDefinitionsByKind.get("SPRINT_STATUS") ?? [];

  return (
    <Select
      value={status}
      disabled={disabled || changeStatus.isPending}
      onValueChange={(v) => changeStatus.mutate({ status: v })}
    >
      <SelectTrigger className="w-36">
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
  );
}
