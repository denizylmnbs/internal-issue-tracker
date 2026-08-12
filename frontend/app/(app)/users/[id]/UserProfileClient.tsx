"use client";

import { useState } from "react";
import Link from "next/link";
import { Pencil, KeyRound } from "lucide-react";
import { useUserDetail, useUserTeamsList, useUserProjectsList } from "@/lib/hooks/useUsers";
import { useSession } from "@/lib/auth/session";
import { isAdmin } from "@/lib/auth/can";
import { RoleChip } from "@/components/shell/chips";
import { ProfileEditDialog } from "@/components/pickers/ProfileEditDialog";
import { ChangePasswordDialog } from "@/components/pickers/ChangePasswordDialog";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { formatDateOnly } from "@/lib/format";

export function UserProfileClient({ userId }: { userId: number }) {
  const { user: caller } = useSession();
  const { data: profile, isLoading } = useUserDetail(userId);
  const { data: teams, isError: teamsError } = useUserTeamsList(userId);
  const { data: projects } = useUserProjectsList(userId);
  const [editOpen, setEditOpen] = useState(false);
  const [pwOpen, setPwOpen] = useState(false);

  if (isLoading || !profile) {
    return (
      <div className="space-y-4 p-6">
        <Skeleton className="h-16 w-16 rounded-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  const canEdit = caller?.id === profile.id || isAdmin(caller);
  const canChangePassword = caller?.id === profile.id;
  const initials = `${profile.name[0]}${profile.surname[0]}`.toUpperCase();

  return (
    <div className="max-w-2xl p-6">
      <div className="mb-6 flex items-start justify-between">
        <div className="flex items-center gap-3">
          <Avatar className="h-14 w-14">
            <AvatarFallback className="bg-secondary text-lg font-medium">{initials}</AvatarFallback>
          </Avatar>
          <div>
            <h1 className="font-heading text-xl font-semibold tracking-tight">
              {profile.name} {profile.surname}
            </h1>
            <div className="mt-0.5 flex items-center gap-2">
              <RoleChip role={profile.role} />
              {!profile.isActive && <span className="text-xs text-rust">Deactivated</span>}
            </div>
          </div>
        </div>
        <div className="flex gap-1">
          {canEdit && (
            <Button variant="outline" size="sm" onClick={() => setEditOpen(true)}>
              <Pencil className="h-3.5 w-3.5" />
              Edit
            </Button>
          )}
          {canChangePassword && (
            <Button variant="outline" size="sm" onClick={() => setPwOpen(true)}>
              <KeyRound className="h-3.5 w-3.5" />
              Change password
            </Button>
          )}
        </div>
      </div>

      <div className="mb-6 grid grid-cols-2 gap-3 text-sm">
        <div>
          <p className="text-xs text-slate">Email</p>
          <p>{profile.email}</p>
        </div>
        <div>
          <p className="text-xs text-slate">Joined</p>
          <p className="font-data">{formatDateOnly(profile.createdAt)}</p>
        </div>
      </div>

      <section className="mb-6">
        <h2 className="mb-2 font-heading text-sm font-semibold">Teams</h2>
        {teamsError ? (
          <p className="text-sm text-rust">Could not load teams. Try refreshing.</p>
        ) : !teams?.content.length ? (
          <p className="text-sm text-slate">Not on a team.</p>
        ) : (
          <div className="divide-y divide-rule rounded border border-rule">
            {teams.content.map((t) => (
              <Link
                key={t.membershipId}
                href={`/teams/${t.teamId}`}
                className="flex items-center justify-between px-3 py-2 hover:bg-secondary"
              >
                <span className="text-sm">{t.teamName}</span>
                <span className="text-xs text-slate">{t.teamField ?? "—"}</span>
              </Link>
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="mb-2 font-heading text-sm font-semibold">Projects</h2>
        {!projects?.content.length ? (
          <p className="text-sm text-slate">Not on a project.</p>
        ) : (
          <div className="divide-y divide-rule rounded border border-rule">
            {projects.content.map((p) => (
              <Link
                key={p.projectId}
                href={`/projects/${p.projectId}`}
                className="flex items-center justify-between px-3 py-2 hover:bg-secondary"
              >
                <span className="text-sm">{p.projectName}</span>
                {!p.directlyAssigned && <span className="text-xs text-slate">via team</span>}
              </Link>
            ))}
          </div>
        )}
      </section>

      {editOpen && <ProfileEditDialog open={editOpen} onOpenChange={setEditOpen} user={profile} />}
      {pwOpen && <ChangePasswordDialog open={pwOpen} onOpenChange={setPwOpen} userId={profile.id} />}
    </div>
  );
}
