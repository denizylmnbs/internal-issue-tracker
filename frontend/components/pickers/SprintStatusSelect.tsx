"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useChangeSprintStatus } from "@/lib/hooks/useSprints";
import { SPRINT_STATUSES, SPRINT_STATUS_LABEL } from "@/lib/api/enums";
import type { SprintStatus } from "@/lib/api/enums";

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
  status: SprintStatus;
  disabled?: boolean;
}) {
  const changeStatus = useChangeSprintStatus(projectId, sprintId);

  return (
    <Select
      value={status}
      disabled={disabled || changeStatus.isPending}
      onValueChange={(v) => changeStatus.mutate({ status: v as SprintStatus })}
    >
      <SelectTrigger className="w-36">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {SPRINT_STATUSES.map((s) => (
          <SelectItem key={s} value={s}>
            {SPRINT_STATUS_LABEL[s]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
