"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useChangeEpicStatus } from "@/lib/hooks/useEpics";
import { useProjectContext } from "@/lib/project/ProjectContext";

export function EpicStatusSelect({
  projectId,
  epicId,
  status,
  disabled,
}: {
  projectId: number;
  epicId: number;
  status: string;
  disabled?: boolean;
}) {
  const changeStatus = useChangeEpicStatus(projectId, epicId);
  const { fieldDefinitionsByKind } = useProjectContext();
  const statuses = fieldDefinitionsByKind.get("EPIC_STATUS") ?? [];

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
