import { ArrowRight } from "lucide-react";
import { UserName } from "@/lib/users/directory";
import { useTeamName } from "@/lib/hooks/useTeamName";
import { useSprintLabel } from "@/lib/hooks/useSprintLabel";
import {
  ISSUE_STATUS_LABEL,
  ISSUE_PRIORITY_LABEL,
  SPRINT_STATUS_LABEL,
  PROJECT_STATUS_LABEL,
} from "@/lib/api/enums";
import type { ActivityActionType } from "@/lib/api/enums";

type Domain = "issue" | "sprint" | "project";

function Raw({ value }: { value: string | null }) {
  return <span className="font-data text-xs">{value ?? "—"}</span>;
}

function EnumValue({ value, label }: { value: string | null; label: Record<string, string> }) {
  if (value == null) return <span className="text-xs text-slate">—</span>;
  return <span className="text-xs">{label[value] ?? value}</span>;
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
        if (domain === "sprint") return <EnumValue value={value} label={SPRINT_STATUS_LABEL} />;
        if (domain === "project") return <EnumValue value={value} label={PROJECT_STATUS_LABEL} />;
        return <EnumValue value={value} label={ISSUE_STATUS_LABEL} />;
      case "PRIORITY_UPDATED":
        return <EnumValue value={value} label={ISSUE_PRIORITY_LABEL} />;
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
