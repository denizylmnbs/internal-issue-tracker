"use client";

import { createContext, useContext, useMemo } from "react";
import { useSession } from "@/lib/auth/session";
import { useFieldDefinitionsList } from "@/lib/hooks/useFieldDefinitions";
import type { FieldDefinitionResponse, FieldKind } from "@/lib/api/types";

/**
 * The two global kinds — PROJECT_STATUS and TEAM_FIELD — fetched once and
 * shared everywhere, the same fix `lib/users/directory.tsx`'s
 * UserDirectoryProvider applies to the user list: nothing here is
 * project-scoped, so there is no per-project context to hang it off, and
 * every consumer re-fetching its own copy would be wasteful for a list this
 * small and this stable.
 */

type GlobalFieldDefinitionsContextValue = {
  isLoading: boolean;
  /** Active rows of one global kind, sorted by sortOrder. */
  listGlobal: (kind: FieldKind) => FieldDefinitionResponse[];
  /** Undefined when the code names no active row — including a soft-deleted
   * one still referenced by old data. Callers must handle that, not assume
   * a match. */
  resolveGlobal: (kind: FieldKind, code: string) => FieldDefinitionResponse | undefined;
};

const GlobalFieldDefinitionsContext = createContext<
  GlobalFieldDefinitionsContextValue | undefined
>(undefined);

export function GlobalFieldDefinitionsProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated } = useSession();
  const { data, isLoading } = useFieldDefinitionsList(null, undefined, {
    enabled: isAuthenticated,
  });

  const value = useMemo<GlobalFieldDefinitionsContextValue>(() => {
    const byKind = new Map<FieldKind, FieldDefinitionResponse[]>();
    const byKindAndCode = new Map<string, FieldDefinitionResponse>();

    for (const def of data ?? []) {
      if (!def.isActive) continue;
      const list = byKind.get(def.kind) ?? [];
      list.push(def);
      byKind.set(def.kind, list);
      byKindAndCode.set(`${def.kind}:${def.code}`, def);
    }
    for (const list of byKind.values()) list.sort((a, b) => a.sortOrder - b.sortOrder);

    return {
      isLoading: isAuthenticated && isLoading,
      listGlobal: (kind) => byKind.get(kind) ?? [],
      resolveGlobal: (kind, code) => byKindAndCode.get(`${kind}:${code}`),
    };
  }, [data, isLoading, isAuthenticated]);

  return (
    <GlobalFieldDefinitionsContext.Provider value={value}>
      {children}
    </GlobalFieldDefinitionsContext.Provider>
  );
}

export function useGlobalFieldDefinitions() {
  const ctx = useContext(GlobalFieldDefinitionsContext);
  if (!ctx) {
    throw new Error(
      "useGlobalFieldDefinitions must be used within GlobalFieldDefinitionsProvider",
    );
  }
  return ctx;
}
