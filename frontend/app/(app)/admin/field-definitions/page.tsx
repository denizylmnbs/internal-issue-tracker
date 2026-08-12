"use client";

import { useSession } from "@/lib/auth/session";
import { isAdmin } from "@/lib/auth/can";
import { EmptyState } from "@/components/shell/EmptyState";
import { FieldDefinitionsSection } from "@/components/settings/FieldDefinitionsSection";

/** The two global kinds — PROJECT_STATUS, TEAM_FIELD — instance-wide, so
 * writes here are ADMIN-only (docs/API.md §4.14), unlike the six per-project
 * kinds a project's own settings page manages. */
export default function AdminFieldDefinitionsPage() {
  const { user } = useSession();

  if (!isAdmin(user)) {
    return (
      <div className="p-6">
        <EmptyState title="Admins only" description="This page is limited to the Admin role." />
      </div>
    );
  }

  return (
    <div className="max-w-2xl p-6">
      <h1 className="mb-1 font-heading text-xl font-semibold tracking-tight">
        Field definitions
      </h1>
      <p className="mb-4 text-sm text-slate">
        Project status and team field are global — every project and team shares this set.
      </p>
      <FieldDefinitionsSection projectId={null} kinds={["PROJECT_STATUS", "TEAM_FIELD"]} canManage />
    </div>
  );
}
