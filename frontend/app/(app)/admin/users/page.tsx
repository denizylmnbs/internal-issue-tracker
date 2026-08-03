"use client";

import { useState } from "react";
import Link from "next/link";
import { KeyRound, UserX } from "lucide-react";
import { useUsersList, useDeactivateUser } from "@/lib/hooks/useUsers";
import { DataTable, type Column } from "@/components/shell/DataTable";
import { RoleChangeSelect } from "@/components/pickers/RoleChangeSelect";
import { ResetPasswordDialog } from "@/components/pickers/ResetPasswordDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
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
import { formatDateOnly } from "@/lib/format";
import type { UserResponse } from "@/lib/api/types";
import { useSession } from "@/lib/auth/session";
import { isAdmin } from "@/lib/auth/can";
import { EmptyState } from "@/components/shell/EmptyState";

export default function AdminUsersPage() {
  const { user } = useSession();
  const [name, setName] = useState("");
  const [resetTarget, setResetTarget] = useState<UserResponse | null>(null);
  const { data, isLoading } = useUsersList({ name: name || undefined, sort: "name,asc", size: 200 });
  const deactivate = useDeactivateUser();

  if (!isAdmin(user)) {
    return (
      <div className="p-6">
        <EmptyState title="Admins only" description="This page is limited to the Admin role." />
      </div>
    );
  }

  const columns: Column<UserResponse>[] = [
    {
      key: "name",
      header: "Name",
      render: (u) => (
        <Link href={`/users/${u.id}`} className="font-medium hover:text-signal">
          {u.name} {u.surname}
        </Link>
      ),
    },
    { key: "email", header: "Email", render: (u) => <span className="text-slate">{u.email}</span> },
    { key: "role", header: "Role", render: (u) => <RoleChangeSelect userId={u.id} currentRole={u.role} /> },
    {
      key: "status",
      header: "Status",
      render: (u) => (
        <Badge variant={u.isActive ? "secondary" : "outline"} className="text-[11px]">
          {u.isActive ? "Active" : "Deactivated"}
        </Badge>
      ),
    },
    { key: "created", header: "Joined", render: (u) => <span className="font-data text-xs">{formatDateOnly(u.createdAt)}</span> },
    {
      key: "actions",
      header: "",
      render: (u) => (
        <div className="flex justify-end gap-1">
          <Button variant="ghost" size="icon" onClick={() => setResetTarget(u)} title="Reset password">
            <KeyRound className="h-4 w-4" />
          </Button>
          {u.isActive && (
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="ghost" size="icon" title="Deactivate">
                  <UserX className="h-4 w-4" />
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Deactivate {u.name} {u.surname}?</AlertDialogTitle>
                  <AlertDialogDescription>
                    Drops them from every team membership and project assignment.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction onClick={() => deactivate.mutate(u.id)}>Deactivate</AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          )}
        </div>
      ),
      headerClassName: "text-right",
      className: "text-right",
    },
  ];

  return (
    <div className="p-6">
      <h1 className="mb-4 font-heading text-xl font-semibold tracking-tight">Users</h1>
      <Input
        placeholder="Filter by name…"
        value={name}
        onChange={(e) => setName(e.target.value)}
        className="mb-4 max-w-64"
      />
      <DataTable columns={columns} rows={data?.content} rowKey={(u) => u.id} isLoading={isLoading} />

      {resetTarget && (
        <ResetPasswordDialog
          open={!!resetTarget}
          onOpenChange={(v) => !v && setResetTarget(null)}
          userId={resetTarget.id}
          userName={`${resetTarget.name} ${resetTarget.surname}`}
        />
      )}
    </div>
  );
}
