import { cn } from "@/lib/utils";
import { ROLE_LABEL } from "@/lib/api/enums";
import type { Role } from "@/lib/api/enums";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useGlobalFieldDefinitions } from "@/lib/fielddef/GlobalFieldDefinitionsProvider";
import { resolveColor } from "@/lib/fielddef/colors";
import type { FieldKind } from "@/lib/api/types";

/**
 * Colour used to be a fixed `Record<Enum, tailwindClass>` per kind — exhaustive
 * over a closed set of codes, which is exactly what a user-defined status set
 * (docs/API.md §2) breaks the moment someone adds one. Every chip below
 * resolves its label and color from the live field definition instead, via
 * `ProjectContext` (the six per-project kinds) or `GlobalFieldDefinitions`
 * (PROJECT_STATUS, TEAM_FIELD) — never both in the same component, since the
 * two live in different providers and a component may not always be under
 * both. A code with no matching definition (soft-deleted, or the project's
 * list hasn't loaded yet) still renders — the raw code as its own label, a
 * palette color keyed on that code — rather than crashing or going blank.
 */

/** For the six per-project kinds — must be rendered under a ProjectProvider. */
function useProjectFieldChip(kind: FieldKind, code: string) {
  const { resolveField } = useProjectContext();
  const def = resolveField(kind, code);
  return { label: def?.label ?? code, color: resolveColor(def, code) };
}

/** For the two global kinds — PROJECT_STATUS, TEAM_FIELD. */
function useGlobalFieldChip(kind: FieldKind, code: string) {
  const { resolveGlobal } = useGlobalFieldDefinitions();
  const def = resolveGlobal(kind, code);
  return { label: def?.label ?? code, color: resolveColor(def, code) };
}

function DotChip({ label, color }: { label: string; color: string }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded border border-rule px-1.5 py-0.5 text-xs font-medium leading-none">
      <span className="h-1.5 w-1.5 shrink-0 rounded-full" style={{ backgroundColor: color }} />
      {label}
    </span>
  );
}

function BorderChip({ label, color }: { label: string; color: string }) {
  return (
    <span
      className="inline-flex items-center rounded border border-rule px-1.5 py-0.5 text-xs font-medium leading-none"
      style={{ color }}
    >
      {label}
    </span>
  );
}

export function IssueStatusChip({ status }: { status: string }) {
  const { label, color } = useProjectFieldChip("ISSUE_STATUS", status);
  return <DotChip label={label} color={color} />;
}

export function PriorityChip({ priority }: { priority: string }) {
  const { label, color } = useProjectFieldChip("ISSUE_PRIORITY", priority);
  return (
    <span className="text-xs font-medium" style={{ color }}>
      {label}
    </span>
  );
}

export function TypeChip({ type }: { type: string }) {
  const { label, color } = useProjectFieldChip("ISSUE_TYPE", type);
  return (
    <span className="text-xs font-medium uppercase tracking-wide" style={{ color }}>
      {label}
    </span>
  );
}

export function UnitChip({ unit }: { unit: string }) {
  const { label, color } = useProjectFieldChip("ISSUE_UNIT", unit);
  return (
    <span
      className="inline-flex items-center rounded-md px-2 py-0.5 text-xs font-semibold leading-none text-white shadow-sm"
      style={{ backgroundColor: color }}
    >
      {label}
    </span>
  );
}

export function ProjectStatusChip({ status }: { status: string }) {
  const { label, color } = useGlobalFieldChip("PROJECT_STATUS", status);
  return <BorderChip label={label} color={color} />;
}

export function SprintStatusChip({ status }: { status: string }) {
  const { label, color } = useProjectFieldChip("SPRINT_STATUS", status);
  return <DotChip label={label} color={color} />;
}

export function EpicStatusChip({ status }: { status: string }) {
  const { label, color } = useProjectFieldChip("EPIC_STATUS", status);
  return <BorderChip label={label} color={color} />;
}

export function RoleChip({ role }: { role: Role }) {
  const color: Record<Role, string> = {
    USER: "text-slate",
    DEVELOPER: "text-ink",
    EDITOR: "text-signal",
    ADMIN: "text-rust",
  };
  return <span className={cn("text-xs font-medium", color[role])}>{ROLE_LABEL[role]}</span>;
}
