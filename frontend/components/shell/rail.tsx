"use client";

import Link from "next/link";
import { PanelLeftClose, PanelLeftOpen, type LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

/**
 * The two navigation rails collapse to icons so a wide view - the board above
 * all, whose columns scroll off the right edge - gets the width back. Both
 * rails render their links and their toggle through these, so a collapsed rail
 * looks and behaves the same wherever it is.
 */

/** A rail entry: label beside the icon when open, tooltip beside it when not. */
export function RailLink({
  href,
  label,
  icon: Icon,
  active,
  collapsed,
}: {
  href: string;
  label: string;
  icon: LucideIcon;
  active: boolean;
  collapsed: boolean;
}) {
  const link = (
    <Link
      href={href}
      // the label is the accessible name when open; when collapsed the visible
      // text is gone, so aria-label carries it for anyone not seeing the tooltip
      aria-label={collapsed ? label : undefined}
      className={cn(
        "flex items-center rounded text-sm transition-colors",
        collapsed ? "justify-center px-0 py-2" : "gap-2.5 px-2.5 py-1.5",
        active ? "bg-accent font-medium text-signal" : "text-ink hover:bg-secondary",
      )}
    >
      <Icon className="h-4 w-4 shrink-0" strokeWidth={2} />
      {!collapsed && <span className="truncate">{label}</span>}
    </Link>
  );

  if (!collapsed) return link;

  return (
    <Tooltip>
      <TooltipTrigger asChild>{link}</TooltipTrigger>
      <TooltipContent side="right">{label}</TooltipContent>
    </Tooltip>
  );
}

/** The collapse/expand control. `what` names the rail in the tooltip and label. */
export function RailToggle({
  collapsed,
  onToggle,
  what,
  className,
}: {
  collapsed: boolean;
  onToggle: () => void;
  what: string;
  className?: string;
}) {
  const action = collapsed ? `Expand ${what}` : `Collapse ${what}`;
  const Icon = collapsed ? PanelLeftOpen : PanelLeftClose;

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <button
          type="button"
          onClick={onToggle}
          aria-label={action}
          aria-expanded={!collapsed}
          className={cn(
            "flex h-7 w-7 shrink-0 items-center justify-center rounded text-slate transition-colors hover:bg-secondary hover:text-ink",
            className,
          )}
        >
          <Icon className="h-4 w-4" strokeWidth={2} />
        </button>
      </TooltipTrigger>
      <TooltipContent side="right">{action}</TooltipContent>
    </Tooltip>
  );
}
