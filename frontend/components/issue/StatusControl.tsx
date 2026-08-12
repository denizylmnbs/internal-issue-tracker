"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useChangeIssueStatus } from "@/lib/hooks/useIssues";
import { useProjectContext } from "@/lib/project/ProjectContext";

export function StatusControl({
  projectId,
  issueId,
  status,
  disabled,
}: {
  projectId: number;
  issueId: number;
  status: string;
  disabled?: boolean;
}) {
  const changeStatus = useChangeIssueStatus(projectId);
  const { fieldDefinitionsByKind } = useProjectContext();
  const statuses = fieldDefinitionsByKind.get("ISSUE_STATUS") ?? [];

  return (
    <Select
      value={status}
      disabled={disabled}
      onValueChange={(v) => changeStatus.mutate({ issueId, body: { status: v } })}
    >
      <SelectTrigger className="w-40">
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
