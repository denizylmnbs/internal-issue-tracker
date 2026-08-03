"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useChangeIssueStatus } from "@/lib/hooks/useIssues";
import { ISSUE_STATUSES, ISSUE_STATUS_LABEL } from "@/lib/api/enums";
import type { IssueStatus } from "@/lib/api/enums";

export function StatusControl({
  projectId,
  issueId,
  status,
  disabled,
}: {
  projectId: number;
  issueId: number;
  status: IssueStatus;
  disabled?: boolean;
}) {
  const changeStatus = useChangeIssueStatus(projectId);

  return (
    <Select
      value={status}
      disabled={disabled}
      onValueChange={(v) => changeStatus.mutate({ issueId, body: { status: v as IssueStatus } })}
    >
      <SelectTrigger className="w-40">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {ISSUE_STATUSES.map((s) => (
          <SelectItem key={s} value={s}>
            {ISSUE_STATUS_LABEL[s]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
