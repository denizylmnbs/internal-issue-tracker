"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { listProjects } from "@/lib/api/endpoints/projects";
import { listTeams } from "@/lib/api/endpoints/teams";
import { FolderKanban, Users } from "lucide-react";

type CommandPaletteContextValue = {
  open: boolean;
  setOpen: (open: boolean) => void;
};

const CommandPaletteContext = createContext<CommandPaletteContextValue | null>(null);

/** Shares the palette's open state with anything that wants to trigger it
 * (e.g. the Topbar button) without relying on a synthetic keydown event. */
export function CommandPaletteProvider({ children }: { children: React.ReactNode }) {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        setOpen((v) => !v);
      }
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, []);

  const value = useMemo(() => ({ open, setOpen }), [open]);

  return <CommandPaletteContext.Provider value={value}>{children}</CommandPaletteContext.Provider>;
}

export function useCommandPalette() {
  const ctx = useContext(CommandPaletteContext);
  if (!ctx) throw new Error("useCommandPalette must be used within a CommandPaletteProvider");
  return ctx;
}

export function CommandPalette() {
  const { open, setOpen } = useCommandPalette();
  const [query, setQuery] = useState("");
  const router = useRouter();

  const { data: projects } = useQuery({
    queryKey: ["command", "projects", query],
    queryFn: () => listProjects({ name: query || undefined, size: 8, sort: "name,asc" }),
    enabled: open,
  });

  const { data: teams } = useQuery({
    queryKey: ["command", "teams", query],
    queryFn: () => listTeams({ name: query || undefined, size: 8, sort: "name,asc" }),
    enabled: open,
  });

  const go = (href: string) => {
    setOpen(false);
    setQuery("");
    router.push(href);
  };

  return (
    <CommandDialog open={open} onOpenChange={setOpen} title="Jump to" description="Search projects and teams">
      <CommandInput placeholder="Jump to a project or team…" value={query} onValueChange={setQuery} />
      <CommandList>
        <CommandEmpty>Nothing matches "{query}".</CommandEmpty>
        {!!projects?.content.length && (
          <CommandGroup heading="Projects">
            {projects.content.map((p) => (
              <CommandItem key={p.id} onSelect={() => go(`/projects/${p.id}`)}>
                <FolderKanban className="h-4 w-4" />
                {p.name}
              </CommandItem>
            ))}
          </CommandGroup>
        )}
        {!!teams?.content.length && (
          <CommandGroup heading="Teams">
            {teams.content.map((t) => (
              <CommandItem key={t.id} onSelect={() => go(`/teams/${t.id}`)}>
                <Users className="h-4 w-4" />
                {t.name}
              </CommandItem>
            ))}
          </CommandGroup>
        )}
      </CommandList>
    </CommandDialog>
  );
}
