import { ArrowRight } from "lucide-react";
import { UserName } from "@/lib/users/directory";
import { useTeamName } from "@/lib/hooks/useTeamName";
import { useSprintLabel } from "@/lib/hooks/useSprintLabel";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useGlobalFieldDefinitions } from "@/lib/fielddef/GlobalFieldDefinitionsProvider";
import type { ActivityActionType } from "@/lib/api/enums";
import type { FieldKind } from "@/lib/api/types";

type Domain = "issue" | "sprint" | "project";

function Raw({ value }: { value: string | null }) {
  return <span className="font-data text-xs">{value ?? "—"}</span>;
}

/** Resolves a raw code against this project's field definitions of `kind`,
 * falling back to the code itself when there's no matching (possibly
 * soft-deleted) row — same contract as the chips in components/shell/chips.tsx. */
function ProjectFieldValue({ kind, value }: { kind: FieldKind; value: string | null }) {
  const { resolveField } = useProjectContext();
  if (value == null) return <span className="text-xs text-slate">—</span>;
  return <span className="text-xs">{resolveField(kind, value)?.label ?? value}</span>;
}

/** As above, against the global field definitions (PROJECT_STATUS). */
function GlobalFieldValue({ kind, value }: { kind: FieldKind; value: string | null }) {
  const { resolveGlobal } = useGlobalFieldDefinitions();
  if (value == null) return <span className="text-xs text-slate">—</span>;
  return <span className="text-xs">{resolveGlobal(kind, value)?.label ?? value}</span>;
}

function UserValue({ value }: { value: string | null }) {
  if (value == null) return <span className="text-xs text-slate">no one</span>;
  return (
    <span className="text-xs">
      <UserName id={Number(value)} />
    </span>
  );
}

function TeamValue({ value }: { value: string | null }) {
  const { name } = useTeamName(value == null ? null : Number(value));
  if (value == null) return <span className="text-xs text-slate">no team</span>;
  return <span className="text-xs">{name ?? "…"}</span>;
}

function SprintValue({ projectId, value }: { projectId: number; value: string | null }) {
  const { name, deleted } = useSprintLabel(projectId, value == null ? null : Number(value));
  if (value == null) return <span className="text-xs text-slate">no sprint</span>;
  if (deleted) return <span className="text-xs text-slate line-through">deleted sprint</span>;
  return <span className="text-xs">{name ?? "…"}</span>;
}

/**
 * Renders the `oldValue → newValue` half of a spine row (docs/API.md §4.12).
 * Values are ids for reference fields and enum names for enum fields — this
 * is the one place that decoding happens, driven by `actionType` plus, for
 * `STATUS_UPDATED` (shared across issue/sprint/project logs), the `domain`
 * the spine is rendering.
 */
export function ValueDelta({
  actionType,
  oldValue,
  newValue,
  domain,
  projectId,
}: {
  actionType: ActivityActionType;
  oldValue: string | null;
  newValue: string | null;
  domain: Domain;
  projectId: number;
}) {
  if (actionType === "CREATED" || actionType === "DELETED") {
    return null;
  }

  // A DETAILS_UPDATED row with both values null is legitimate — only a name
  // change carries values; description changes don't fit and aren't
  // truncated into something that looks like one. Render it as its own
  // designed state, never as "null → null".
  if (actionType === "DETAILS_UPDATED" && oldValue == null && newValue == null) {
    return <span className="text-xs italic text-slate">details moved</span>;
  }

  const renderValue = (value: string | null) => {
    switch (actionType) {
      case "STATUS_UPDATED":
        if (domain === "sprint") return <ProjectFieldValue kind="SPRINT_STATUS" value={value} />;
        if (domain === "project") return <GlobalFieldValue kind="PROJECT_STATUS" value={value} />;
        return <ProjectFieldValue kind="ISSUE_STATUS" value={value} />;
      case "PRIORITY_UPDATED":
        return <ProjectFieldValue kind="ISSUE_PRIORITY" value={value} />;
      case "ASSIGNEE_USER_UPDATED":
      case "LEADER_UPDATED":
      case "USER_ADDED":
      case "USER_REMOVED":
        return <UserValue value={value} />;
      case "ASSIGNEE_TEAM_UPDATED":
      case "TEAM_ADDED":
      case "TEAM_REMOVED":
        return <TeamValue value={value} />;
      case "SPRINT_UPDATED":
        return <SprintValue projectId={projectId} value={value} />;
      default:
        return <Raw value={value} />;
    }
  };

  return (
    <span className="inline-flex items-center gap-1.5">
      {renderValue(oldValue)}
      <ArrowRight className="h-3 w-3 text-slate" />
      {renderValue(newValue)}
    </span>
  );
}
