"use client";

import { Search } from "lucide-react";
import { useCommandPalette } from "@/components/shell/CommandPalette";

export function Topbar() {
  const { setOpen } = useCommandPalette();

  return (
    <div className="flex h-12 shrink-0 items-center justify-end border-b border-rule px-4">
      <button
        onClick={() => setOpen(true)}
        className="flex items-center gap-2 rounded border border-rule px-2.5 py-1 text-xs text-slate hover:bg-secondary"
      >
        <Search className="h-3.5 w-3.5" />
        Jump to…
        <kbd className="ml-2 rounded border border-rule bg-secondary px-1 font-data text-[10px]">
          ⌘K
        </kbd>
      </button>
    </div>
  );
}
