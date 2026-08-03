"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useChangeEpicStatus } from "@/lib/hooks/useEpics";
import { EPIC_STATUSES, EPIC_STATUS_LABEL } from "@/lib/api/enums";
import type { EpicStatus } from "@/lib/api/enums";

export function EpicStatusSelect({
  projectId,
  epicId,
  status,
  disabled,
}: {
  projectId: number;
  epicId: number;
  status: EpicStatus;
  disabled?: boolean;
}) {
  const changeStatus = useChangeEpicStatus(projectId, epicId);

  return (
    <Select
      value={status}
      disabled={disabled || changeStatus.isPending}
      onValueChange={(v) => changeStatus.mutate({ status: v as EpicStatus })}
    >
      <SelectTrigger className="w-36">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {EPIC_STATUSES.map((s) => (
          <SelectItem key={s} value={s}>
            {EPIC_STATUS_LABEL[s]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
