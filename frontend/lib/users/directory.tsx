"use client";

import { createContext, useContext, useMemo } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { listUsers, getUser } from "@/lib/api/endpoints/users";
import type { UserResponse } from "@/lib/api/types";
import { useSession } from "@/lib/auth/session";

/**
 * The fix for docs/API.md §5 note 1: there is no batch user-lookup endpoint,
 * and ids (`reporterId`, `assigneeUserId`, activity `userId`, …) are bare
 * integers everywhere. Fetch the first 200 users once, keep an id→user map,
 * and fall back to a single-id fetch (cached forever — users don't rename
 * themselves mid-session) for anyone off that page.
 */

type DirectoryContextValue = {
  byId: Map<number, UserResponse>;
  isLoading: boolean;
};

const DirectoryContext = createContext<DirectoryContextValue>({
  byId: new Map(),
  isLoading: true,
});

export function UserDirectoryProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useSession();

  const { data, isLoading } = useQuery({
    queryKey: ["users", "directory"],
    queryFn: () => listUsers({ size: 200, sort: "name,asc" }),
    enabled: isAuthenticated,
    staleTime: 5 * 60_000,
  });

  const byId = useMemo(() => {
    const map = new Map<number, UserResponse>();
    for (const u of data?.content ?? []) map.set(u.id, u);
    return map;
  }, [data]);

  return (
    <DirectoryContext.Provider value={{ byId, isLoading }}>
      {children}
    </DirectoryContext.Provider>
  );
}

export function useUserDirectory() {
  return useContext(DirectoryContext);
}

/** For an id not on the first page — cached forever once fetched. */
export function useUser(id: number | null | undefined) {
  const { byId } = useUserDirectory();
  const known = id != null ? byId.get(id) : undefined;

  const { data } = useQuery({
    queryKey: ["users", id],
    queryFn: () => getUser(id as number),
    enabled: id != null && !known,
    staleTime: Infinity,
  });

  return known ?? data;
}

/** Renders "Name Surname" for an id, falling back to a placeholder while the
 * directory or a stray lookup is in flight. Never a bare id. */
export function UserName({ id }: { id: number | null | undefined }) {
  const user = useUser(id);
  if (id == null) return <span className="text-slate">Unassigned</span>;
  if (!user) return <span className="text-slate">…</span>;
  return (
    <span>
      {user.name} {user.surname}
    </span>
  );
}
