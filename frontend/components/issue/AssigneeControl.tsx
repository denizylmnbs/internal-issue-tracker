"use client";

import { useState } from "react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Button } from "@/components/ui/button";
import { UserPicker } from "@/components/pickers/UserPicker";
import { TeamPicker } from "@/components/pickers/TeamPicker";
import { UserName } from "@/lib/users/directory";
import { useTeamName } from "@/lib/hooks/useTeamName";
import { useChangeIssueAssignee, useClearIssueAssignee } from "@/lib/hooks/useIssues";
import { Users } from "lucide-react";

/**
 * One control owns both assignee fields together — the fix for docs/API.md
 * §5 note 3: PATCH .../assignee replaces the pair, so sending one alone
 * clears the other. Saving here always sends both current values.
 */
export function AssigneeControl({
  projectId,
  issueId,
  assigneeUserId,
  assigneeTeamId,
  disabled,
}: {
  projectId: number;
  issueId: number;
  assigneeUserId: number | null;
  assigneeTeamId: number | null;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [draftUser, setDraftUser] = useState(assigneeUserId);
  const [draftTeam, setDraftTeam] = useState(assigneeTeamId);
  const changeAssignee = useChangeIssueAssignee(projectId, issueId);
  const clearAssignee = useClearIssueAssignee(projectId, issueId);
  const { name: teamName } = useTeamName(assigneeTeamId);

  const save = () => {
    changeAssignee.mutate(
      { assigneeUserId: draftUser, assigneeTeamId: draftTeam },
      { onSuccess: () => setOpen(false) },
    );
  };

  return (
    <Popover
      open={open}
      onOpenChange={(v) => {
        setOpen(v);
        if (v) {
          setDraftUser(assigneeUserId);
          setDraftTeam(assigneeTeamId);
        }
      }}
    >
      <PopoverTrigger asChild>
        <button
          disabled={disabled}
          className="flex w-full items-center gap-2 rounded border border-rule px-2.5 py-1.5 text-left text-sm hover:border-signal disabled:opacity-60"
        >
          <Users className="h-3.5 w-3.5 shrink-0 text-slate" />
          <span className="min-w-0 flex-1 truncate">
            <UserName id={assigneeUserId} />
            {assigneeTeamId && (
              <span className="text-slate"> · {teamName ?? "…"}</span>
            )}
          </span>
        </button>
      </PopoverTrigger>
      <PopoverContent className="w-72 space-y-3" align="start">
        <div className="space-y-1.5">
          <p className="text-xs font-medium text-slate">Person</p>
          <UserPicker value={draftUser} onChange={setDraftUser} placeholder="Unassigned" />
        </div>
        <div className="space-y-1.5">
          <p className="text-xs font-medium text-slate">Team</p>
          <TeamPicker value={draftTeam} onChange={setDraftTeam} />
        </div>
        <div className="flex justify-between pt-1">
          <Button
            size="sm"
            variant="ghost"
            onClick={() => clearAssignee.mutate(undefined, { onSuccess: () => setOpen(false) })}
          >
            Clear both
          </Button>
          <Button size="sm" onClick={save} disabled={changeAssignee.isPending}>
            Save
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
