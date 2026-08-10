import { cn } from "@/lib/utils";
import {
  ISSUE_STATUS_LABEL,
  ISSUE_PRIORITY_LABEL,
  ISSUE_TYPE_LABEL,
  ISSUE_RESOLVING_UNIT_LABEL,
  PROJECT_STATUS_LABEL,
  SPRINT_STATUS_LABEL,
  EPIC_STATUS_LABEL,
  ROLE_LABEL,
} from "@/lib/api/enums";
import type {
  IssueStatus,
  IssuePriority,
  IssueType,
  IssueResolvingUnit,
  ProjectStatus,
  SprintStatus,
  EpicStatus,
  Role,
} from "@/lib/api/enums";

/**
 * Colour is reserved for work state — see app/globals.css. Every chip here
 * is a small text label with a dot or border in a semantic hue, never a
 * filled decorative badge. The one exception is UnitChip: each resolving
 * unit (backend/frontend/iOS/Android) gets a fixed, filled team colour so
 * it reads at a glance across the board and issue views.
 */

function Chip({
  label,
  className,
}: {
  label: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded border border-rule px-1.5 py-0.5 text-xs font-medium leading-none",
        className,
      )}
    >
      {label}
    </span>
  );
}

const ISSUE_STATUS_DOT: Record<IssueStatus, string> = {
  BACKLOG: "bg-slate",
  TODO: "bg-slate",
  IN_PROGRESS: "bg-signal",
  IN_REVIEW: "bg-amber",
  DONE: "bg-moss",
  ON_HOLD: "bg-amber",
  CANCELLED: "bg-rust",
};

export function IssueStatusChip({ status }: { status: IssueStatus }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded border border-rule px-1.5 py-0.5 text-xs font-medium leading-none">
      <span className={cn("h-1.5 w-1.5 rounded-full", ISSUE_STATUS_DOT[status])} />
      {ISSUE_STATUS_LABEL[status]}
    </span>
  );
}

const PRIORITY_COLOR: Record<IssuePriority, string> = {
  LOW: "text-slate",
  MEDIUM: "text-ink",
  HIGH: "text-amber",
  CRITICAL: "text-rust",
};

export function PriorityChip({ priority }: { priority: IssuePriority }) {
  return (
    <span className={cn("text-xs font-medium", PRIORITY_COLOR[priority])}>
      {ISSUE_PRIORITY_LABEL[priority]}
    </span>
  );
}

const TYPE_COLOR: Record<IssueType, string> = {
  BUG: "text-rust",
  FEATURE: "text-signal",
  STORY: "text-moss",
  TASK: "text-slate",
  ENHANCEMENT: "text-signal",
  REFACTOR: "text-slate",
};

export function TypeChip({ type }: { type: IssueType }) {
  return (
    <span className={cn("text-xs font-medium uppercase tracking-wide", TYPE_COLOR[type])}>
      {ISSUE_TYPE_LABEL[type]}
    </span>
  );
}

const UNIT_COLOR: Record<IssueResolvingUnit, string> = {
  BACKEND: "bg-signal text-signal-foreground",
  FRONTEND: "bg-moss text-moss-foreground",
  IOS: "bg-slate text-slate-foreground",
  ANDROID: "bg-amber text-amber-foreground",
};

export function UnitChip({ unit }: { unit: IssueResolvingUnit }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md px-2 py-0.5 text-xs font-semibold leading-none shadow-sm",
        UNIT_COLOR[unit],
      )}
    >
      {ISSUE_RESOLVING_UNIT_LABEL[unit]}
    </span>
  );
}

export function ProjectStatusChip({ status }: { status: ProjectStatus }) {
  return <Chip label={PROJECT_STATUS_LABEL[status]} />;
}

export function SprintStatusChip({ status }: { status: SprintStatus }) {
  const dot: Record<SprintStatus, string> = {
    TODO: "bg-slate",
    IN_PROGRESS: "bg-signal",
    TESTING: "bg-amber",
    COMPLETED: "bg-moss",
  };
  return (
    <span className="inline-flex items-center gap-1.5 rounded border border-rule px-1.5 py-0.5 text-xs font-medium leading-none">
      <span className={cn("h-1.5 w-1.5 rounded-full", dot[status])} />
      {SPRINT_STATUS_LABEL[status]}
    </span>
  );
}

export function EpicStatusChip({ status }: { status: EpicStatus }) {
  return <Chip label={EPIC_STATUS_LABEL[status]} />;
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
