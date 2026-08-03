"use client";

import { createContext, useCallback, useContext } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { getMe } from "@/lib/api/endpoints/users";
import type { UserResponse } from "@/lib/api/types";

/**
 * "Who am I", resolved from GET /api/auth/me (docs/API.md §4.1) — the only
 * place a role is ever read. It is never cached beyond a page's lifetime in
 * spirit: a short staleTime, and every mutation wrapper (see
 * lib/hooks/useMutationWithAuth.ts, added per-resource) invalidates it on a
 * 403 so a role change elsewhere takes effect promptly.
 */

type SessionContextValue = {
  user: UserResponse | undefined;
  isLoading: boolean;
  isAuthenticated: boolean;
  refetch: () => void;
  logout: () => Promise<void>;
};

const SessionContext = createContext<SessionContextValue | undefined>(undefined);

export const SESSION_QUERY_KEY = ["session", "me"] as const;

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();
  const router = useRouter();

  const { data: user, isLoading, refetch } = useQuery({
    queryKey: SESSION_QUERY_KEY,
    queryFn: getMe,
    staleTime: 60_000,
    retry: false,
  });

  const logout = useCallback(async () => {
    await fetch("/api/session", { method: "DELETE" });
    queryClient.clear();
    router.push("/login");
  }, [queryClient, router]);

  return (
    <SessionContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user,
        refetch: () => void refetch(),
        logout,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSession must be used within SessionProvider");
  return ctx;
}
