"use client";

import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuLabel,
  ContextMenuSeparator,
  ContextMenuSub,
  ContextMenuSubContent,
  ContextMenuSubTrigger,
  ContextMenuTrigger,
} from "@/components/ui/context-menu";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { ChevronDown } from "lucide-react";
import { IssueMenuItems, type MenuParts } from "./IssueMenuItems";
import type { IssueResponse } from "@/lib/api/types";

const CONTEXT_PARTS: MenuParts = {
  Label: ContextMenuLabel,
  Item: ContextMenuItem,
  Separator: ContextMenuSeparator,
  Sub: ContextMenuSub,
  SubTrigger: ContextMenuSubTrigger,
  SubContent: ContextMenuSubContent,
};

const DROPDOWN_PARTS: MenuParts = {
  Label: DropdownMenuLabel,
  Item: DropdownMenuItem,
  Separator: DropdownMenuSeparator,
  Sub: DropdownMenuSub,
  SubTrigger: DropdownMenuSubTrigger,
  SubContent: DropdownMenuSubContent,
};

type Shared = {
  projectId: number;
  targets: IssueResponse[];
  onEdit: (issue: IssueResponse) => void;
  onDelete: (targets: IssueResponse[]) => void;
};

/** Right-click anywhere on `children`. Fast, but invisible and unreachable
 * without a mouse — always pair it with the toolbar below. */
export function IssueContextMenu({
  children,
  onOpen,
  ...shared
}: Shared & { children: React.ReactNode; onOpen?: () => void }) {
  return (
    <ContextMenu
      onOpenChange={(open) => {
        if (open) onOpen?.();
      }}
    >
      <ContextMenuTrigger asChild>{children}</ContextMenuTrigger>
      <ContextMenuContent>
        <IssueMenuItems parts={CONTEXT_PARTS} {...shared} />
      </ContextMenuContent>
    </ContextMenu>
  );
}

/** The same menu behind a button — the keyboard, touch and discoverable path. */
export function IssueActionsMenu(shared: Shared) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button size="sm" variant="outline">
          Edit {shared.targets.length} selected
          <ChevronDown className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-auto min-w-40">
        <IssueMenuItems parts={DROPDOWN_PARTS} {...shared} />
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
