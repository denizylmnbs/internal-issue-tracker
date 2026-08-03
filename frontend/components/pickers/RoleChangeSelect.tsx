"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useChangeUserRole } from "@/lib/hooks/useUsers";
import { useSession } from "@/lib/auth/session";
import { ROLES, ROLE_LABEL, ROLE_RANK } from "@/lib/api/enums";
import type { Role } from "@/lib/api/enums";

/**
 * Encodes the outranking rule from docs/API.md §3 as UI guidance: the caller
 * must strictly outrank both the target's current role and the requested
 * new role. Since an ADMIN never outranks another ADMIN, this also covers
 * "can't change your own role" and "can't change another admin" without a
 * special case — targeting self or another admin means the current-role
 * check already fails. The backend is the real gate; this only avoids
 * offering a change that's certain to 403.
 */
export function RoleChangeSelect({
  userId,
  currentRole,
}: {
  userId: number;
  currentRole: Role;
}) {
  const { user: caller } = useSession();
  const changeRole = useChangeUserRole(userId);

  const callerRank = caller ? ROLE_RANK[caller.role] : -1;
  const canChangeThisUser = callerRank > ROLE_RANK[currentRole];
  const selectableRoles = ROLES.filter((r) => callerRank > ROLE_RANK[r]);

  if (!canChangeThisUser) {
    return <span className="text-xs text-slate">{ROLE_LABEL[currentRole]}</span>;
  }

  return (
    <Select
      value={currentRole}
      disabled={changeRole.isPending}
      onValueChange={(v) => changeRole.mutate({ newRole: v as Role })}
    >
      <SelectTrigger className="h-8 w-32 text-xs">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {ROLES.map((r) => (
          <SelectItem key={r} value={r} disabled={r !== currentRole && !selectableRoles.includes(r)}>
            {ROLE_LABEL[r]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
