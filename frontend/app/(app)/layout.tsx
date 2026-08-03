"use client";

import { AppRail } from "@/components/shell/AppRail";
import { Topbar } from "@/components/shell/Topbar";
import { CommandPalette, CommandPaletteProvider } from "@/components/shell/CommandPalette";
import { useSession } from "@/lib/auth/session";
import { Skeleton } from "@/components/ui/skeleton";

export default function AppShellLayout({ children }: { children: React.ReactNode }) {
  const { isLoading, isAuthenticated } = useSession();

  // proxy.ts already redirected unauthenticated requests to /login. This is
  // just the brief window while GET /api/auth/me resolves after a fresh
  // cookie — not a second auth gate.
  if (isLoading || !isAuthenticated) {
    return (
      <div className="flex h-screen">
        <div className="w-56 shrink-0 border-r border-rule p-4">
          <Skeleton className="h-6 w-32" />
        </div>
        <div className="flex-1 p-6">
          <Skeleton className="h-8 w-64" />
        </div>
      </div>
    );
  }

  return (
    <CommandPaletteProvider>
      <div className="flex h-screen overflow-hidden">
        <AppRail />
        <div className="flex min-w-0 flex-1 flex-col">
          <Topbar />
          <main className="flex-1 overflow-y-auto">{children}</main>
        </div>
        <CommandPalette />
      </div>
    </CommandPaletteProvider>
  );
}
