"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Pencil, Trash2, UserPlus, X } from "lucide-react";
import { useTeam, useTeamMembers, useAddTeamMember, useRemoveTeamMember, useChangeTeamLeader, useDeleteTeam } from "@/lib/hooks/useTeams";
import { useSession } from "@/lib/auth/session";
import { editorOrTeamLeader, isEditorOrAbove } from "@/lib/auth/can";
import { UserName } from "@/lib/users/directory";
import { UserPicker } from "@/components/pickers/UserPicker";
import { TeamFormDialog } from "@/components/pickers/TeamFormDialog";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { formatDateOnly } from "@/lib/format";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";

export function TeamDetailClient({ teamId }: { teamId: number }) {
  const router = useRouter();
  const { user } = useSession();
  const { data: team, isLoading } = useTeam(teamId);
  const { data: members, isLoading: loadingMembers, isError: membersError } = useTeamMembers(teamId);
  const addMember = useAddTeamMember(teamId);
  const removeMember = useRemoveTeamMember(teamId);
  const changeLeader = useChangeTeamLeader(teamId);
  const deleteTeam = useDeleteTeam();

  const [editOpen, setEditOpen] = useState(false);
  const [leaderOpen, setLeaderOpen] = useState(false);
  const [draftLeader, setDraftLeader] = useState<number | null>(null);
  const [newMember, setNewMember] = useState<number | null>(null);

  if (isLoading || !team) {
    return (
      <div className="space-y-4 p-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  const canManage = editorOrTeamLeader(user, team.leaderId);

  return (
    <div className="max-w-2xl p-6">
      <div className="mb-1 flex items-start justify-between">
        <h1 className="font-heading text-xl font-semibold tracking-tight">{team.name}</h1>
        <div className="flex gap-1">
          {canManage && (
            <Button variant="ghost" size="icon" onClick={() => setEditOpen(true)}>
              <Pencil className="h-4 w-4" />
            </Button>
          )}
          {isEditorOrAbove(user) && (
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="ghost" size="icon">
                  <Trash2 className="h-4 w-4" />
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Delete "{team.name}"?</AlertDialogTitle>
                  <AlertDialogDescription>
                    This also drops the team's project assignments.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={() => deleteTeam.mutate(teamId, { onSuccess: () => router.push("/teams") })}
                  >
                    Delete
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          )}
        </div>
      </div>
      <p className="mb-6 text-sm text-slate">{team.field ?? "No field set"}</p>

      <div className="mb-6 flex items-center gap-2">
        <span className="text-xs font-medium text-slate">Leader:</span>
        {isEditorOrAbove(user) ? (
          <Popover
            open={leaderOpen}
            onOpenChange={(v) => {
              setLeaderOpen(v);
              if (v) setDraftLeader(team.leaderId);
            }}
          >
            <PopoverTrigger asChild>
              <button className="text-sm text-signal hover:underline">
                <UserName id={team.leaderId} />
              </button>
            </PopoverTrigger>
            <PopoverContent className="w-72 space-y-2" align="start">
              <UserPicker value={draftLeader} onChange={setDraftLeader} eligibleOnly />
              <Button
                size="sm"
                className="w-full"
                disabled={draftLeader == null}
                onClick={() =>
                  draftLeader != null &&
                  changeLeader.mutate({ leaderId: draftLeader }, { onSuccess: () => setLeaderOpen(false) })
                }
              >
                Save
              </Button>
            </PopoverContent>
          </Popover>
        ) : (
          <span className="text-sm"><UserName id={team.leaderId} /></span>
        )}
      </div>

      <div className="mb-2 flex items-center justify-between">
        <h2 className="font-heading text-sm font-semibold">Members</h2>
        {canManage && (
          <Popover>
            <PopoverTrigger asChild>
              <Button size="sm" variant="outline">
                <UserPlus className="h-3.5 w-3.5" />
                Add member
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-72 space-y-2" align="end">
              <UserPicker value={newMember} onChange={setNewMember} eligibleOnly placeholder="Choose a person…" />
              <Button
                size="sm"
                className="w-full"
                disabled={newMember == null || addMember.isPending}
                onClick={() =>
                  newMember != null &&
                  addMember.mutate({ userId: newMember }, { onSuccess: () => setNewMember(null) })
                }
              >
                Add to team
              </Button>
            </PopoverContent>
          </Popover>
        )}
      </div>

      {loadingMembers ? (
        <Skeleton className="h-24 w-full" />
      ) : membersError ? (
        <p className="text-sm text-rust">Couldn't load members. Try refreshing.</p>
      ) : !members?.content.filter((m) => m.isActive).length ? (
        <p className="text-sm text-slate">No members yet.</p>
      ) : (
        <div className="divide-y divide-rule rounded border border-rule">
          {members.content
            .filter((m) => m.isActive)
            .map((m) => (
              <div key={m.id} className="flex items-center justify-between px-3 py-2">
                <div>
                  <p className="text-sm">
                    <UserName id={m.userId} />
                  </p>
                  <p className="font-data text-xs text-slate">joined {formatDateOnly(m.joinedAt)}</p>
                </div>
                {canManage && (
                  <Button variant="ghost" size="icon" onClick={() => removeMember.mutate(m.userId)}>
                    <X className="h-4 w-4" />
                  </Button>
                )}
              </div>
            ))}
        </div>
      )}

      {editOpen && <TeamFormDialog open={editOpen} onOpenChange={setEditOpen} team={team} />}
    </div>
  );
}
