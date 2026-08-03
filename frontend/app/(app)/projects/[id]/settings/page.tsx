"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Pencil, Trash2, X, UserPlus, Users as UsersIcon } from "lucide-react";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useSession } from "@/lib/auth/session";
import { isEditorOrAbove } from "@/lib/auth/can";
import {
  useProjectMembers,
  useProjectTeams,
  useChangeProjectStatus,
  useChangeProjectLeader,
  useRemoveProjectLeader,
  useDeleteProject,
} from "@/lib/hooks/useProjects";
import {
  useAddProjectMember,
  useRemoveProjectMember,
  useAddProjectTeam,
  useRemoveProjectTeam,
} from "@/lib/hooks/useProjectMembership";
import { UserName } from "@/lib/users/directory";
import { UserPicker } from "@/components/pickers/UserPicker";
import { TeamPicker } from "@/components/pickers/TeamPicker";
import { ProjectEditDialog } from "@/components/pickers/ProjectEditDialog";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
import { PROJECT_STATUSES, PROJECT_STATUS_LABEL } from "@/lib/api/enums";
import type { ProjectStatus } from "@/lib/api/enums";
import { formatDateOnly } from "@/lib/format";
import { useTeamName } from "@/lib/hooks/useTeamName";

export default function ProjectSettingsPage() {
  const router = useRouter();
  const { user } = useSession();
  const { projectId, project, canManage } = useProjectContext();

  const { data: members, isLoading: loadingMembers } = useProjectMembers(projectId);
  const { data: teams, isLoading: loadingTeams } = useProjectTeams(projectId);
  const changeStatus = useChangeProjectStatus(projectId);
  const changeLeader = useChangeProjectLeader(projectId);
  const removeLeader = useRemoveProjectLeader(projectId);
  const deleteProject = useDeleteProject();
  const addMember = useAddProjectMember(projectId);
  const removeMember = useRemoveProjectMember(projectId);
  const addTeam = useAddProjectTeam(projectId);
  const removeTeam = useRemoveProjectTeam(projectId);

  const [editOpen, setEditOpen] = useState(false);
  const [leaderOpen, setLeaderOpen] = useState(false);
  const [draftLeader, setDraftLeader] = useState<number | null>(null);
  const [newMember, setNewMember] = useState<number | null>(null);
  const [newTeam, setNewTeam] = useState<number | null>(null);

  if (!project) {
    return (
      <div className="space-y-4 p-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  const isEditor = isEditorOrAbove(user);

  return (
    <div className="max-w-2xl space-y-8 p-6">
      <div>
        <div className="mb-4 flex items-center justify-between">
          <h1 className="font-heading text-xl font-semibold tracking-tight">Settings</h1>
          {canManage && (
            <Button size="sm" variant="outline" onClick={() => setEditOpen(true)}>
              <Pencil className="h-3.5 w-3.5" />
              Edit details
            </Button>
          )}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="rounded border border-rule p-3">
            <p className="mb-1 text-xs text-slate">Status</p>
            {canManage ? (
              <Select
                value={project.status}
                onValueChange={(v) => changeStatus.mutate({ status: v as ProjectStatus })}
              >
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PROJECT_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>{PROJECT_STATUS_LABEL[s]}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            ) : (
              <p className="text-sm">{PROJECT_STATUS_LABEL[project.status]}</p>
            )}
          </div>

          <div className="rounded border border-rule p-3">
            <p className="mb-1 text-xs text-slate">Leader</p>
            {isEditor ? (
              <div className="flex items-center gap-2">
                <Popover
                  open={leaderOpen}
                  onOpenChange={(v) => { setLeaderOpen(v); if (v) setDraftLeader(project.leaderId); }}
                >
                  <PopoverTrigger asChild>
                    <button className="text-sm text-signal hover:underline">
                      <UserName id={project.leaderId} />
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
                {project.leaderId != null && (
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <button className="text-xs text-slate hover:text-rust">remove</button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Leave this project without a leader?</AlertDialogTitle>
                        <AlertDialogDescription>
                          Until a new one is named, only an Editor can act on it.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={() => removeLeader.mutate()}>Remove leader</AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                )}
              </div>
            ) : (
              <p className="text-sm"><UserName id={project.leaderId} /></p>
            )}
          </div>
        </div>
      </div>

      <div>
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
                  disabled={newMember == null}
                  onClick={() => newMember != null && addMember.mutate(newMember, { onSuccess: () => setNewMember(null) })}
                >
                  Add
                </Button>
              </PopoverContent>
            </Popover>
          )}
        </div>
        {loadingMembers ? (
          <Skeleton className="h-16 w-full" />
        ) : !members?.content.filter((m) => m.isActive).length ? (
          <p className="text-sm text-slate">No directly assigned members. People reached through a team still count as participants.</p>
        ) : (
          <div className="divide-y divide-rule rounded border border-rule">
            {members.content.filter((m) => m.isActive).map((m) => (
              <div key={m.id} className="flex items-center justify-between px-3 py-2">
                <div>
                  <p className="text-sm"><UserName id={m.userId} /></p>
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
      </div>

      <div>
        <div className="mb-2 flex items-center justify-between">
          <h2 className="font-heading text-sm font-semibold">Assigned teams</h2>
          {canManage && (
            <Popover>
              <PopoverTrigger asChild>
                <Button size="sm" variant="outline">
                  <UsersIcon className="h-3.5 w-3.5" />
                  Assign team
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-72 space-y-2" align="end">
                <TeamPicker value={newTeam} onChange={setNewTeam} placeholder="Choose a team…" />
                <Button
                  size="sm"
                  className="w-full"
                  disabled={newTeam == null}
                  onClick={() => newTeam != null && addTeam.mutate(newTeam, { onSuccess: () => setNewTeam(null) })}
                >
                  Assign
                </Button>
              </PopoverContent>
            </Popover>
          )}
        </div>
        {loadingTeams ? (
          <Skeleton className="h-16 w-full" />
        ) : !teams?.content.filter((t) => t.isActive).length ? (
          <p className="text-sm text-slate">No teams assigned yet.</p>
        ) : (
          <div className="divide-y divide-rule rounded border border-rule">
            {teams.content.filter((t) => t.isActive).map((t) => (
              <TeamRow key={t.id} teamId={t.teamId} assignedAt={t.assignedAt} canManage={!!canManage} onRemove={() => removeTeam.mutate(t.teamId)} />
            ))}
          </div>
        )}
      </div>

      {isEditor && (
        <div className="border-t border-rule pt-6">
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="destructive" size="sm">
                <Trash2 className="h-3.5 w-3.5" />
                Delete project
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Delete "{project.name}"?</AlertDialogTitle>
                <AlertDialogDescription>
                  This soft-deletes the project — it accepts no new members, sprints, epics or issues.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction onClick={() => deleteProject.mutate(projectId, { onSuccess: () => router.push("/projects") })}>
                  Delete
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      )}

      {editOpen && <ProjectEditDialog open={editOpen} onOpenChange={setEditOpen} project={project} />}
    </div>
  );
}

function TeamRow({
  teamId,
  assignedAt,
  canManage,
  onRemove,
}: {
  teamId: number;
  assignedAt: string;
  canManage: boolean;
  onRemove: () => void;
}) {
  const { name } = useTeamName(teamId);
  return (
    <div className="flex items-center justify-between px-3 py-2">
      <div>
        <p className="text-sm">{name ?? "…"}</p>
        <p className="font-data text-xs text-slate">assigned {formatDateOnly(assignedAt)}</p>
      </div>
      {canManage && (
        <Button variant="ghost" size="icon" onClick={onRemove}>
          <X className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}
