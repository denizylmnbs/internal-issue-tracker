"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutGrid, FolderKanban, Users, ShieldCheck, LogOut, Tags } from "lucide-react";
import { cn } from "@/lib/utils";
import { useSession } from "@/lib/auth/session";
import { isAdmin } from "@/lib/auth/can";
import { RoleChip } from "./chips";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";

const NAV = [
  { href: "/", label: "My work", icon: LayoutGrid, exact: true },
  { href: "/projects", label: "Projects", icon: FolderKanban },
  { href: "/teams", label: "Teams", icon: Users },
];

const ADMIN_NAV = [
  { href: "/admin/users", label: "Users", icon: ShieldCheck },
  { href: "/admin/field-definitions", label: "Field definitions", icon: Tags },
];

export function AppRail() {
  const pathname = usePathname();
  const { user, logout } = useSession();

  const initials = user ? `${user.name[0]}${user.surname[0]}`.toUpperCase() : "";

  return (
    <aside className="flex h-full w-56 shrink-0 flex-col border-r border-rule bg-sidebar">
      <Link
        href="/"
        className="flex h-12 items-center gap-2 border-b border-rule px-4 hover:bg-secondary"
      >
        <div className="flex h-6 w-6 items-center justify-center rounded bg-signal font-heading text-xs font-bold text-signal-foreground">
          I
        </div>
        <span className="font-heading text-sm font-semibold tracking-tight">
          Issue Tracker
        </span>
      </Link>

      <nav className="flex-1 space-y-0.5 px-2 py-3">
        {NAV.map(({ href, label, icon: Icon, exact }) => {
          const active = exact ? pathname === href : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-2.5 rounded px-2.5 py-1.5 text-sm transition-colors",
                active
                  ? "bg-accent font-medium text-signal"
                  : "text-ink hover:bg-secondary",
              )}
            >
              <Icon className="h-4 w-4" strokeWidth={2} />
              {label}
            </Link>
          );
        })}
        {isAdmin(user) && (
          <>
            <p className="mt-3 px-2.5 text-xs font-medium uppercase tracking-wide text-slate">
              Admin
            </p>
            {ADMIN_NAV.map(({ href, label, icon: Icon }) => {
              const active = pathname.startsWith(href);
              return (
                <Link
                  key={href}
                  href={href}
                  className={cn(
                    "flex items-center gap-2.5 rounded px-2.5 py-1.5 text-sm transition-colors",
                    active
                      ? "bg-accent font-medium text-signal"
                      : "text-ink hover:bg-secondary",
                  )}
                >
                  <Icon className="h-4 w-4" strokeWidth={2} />
                  {label}
                </Link>
              );
            })}
          </>
        )}
      </nav>

      {user && (
        <div className="border-t border-rule p-2">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="flex w-full items-center gap-2.5 rounded px-2 py-2 text-left hover:bg-secondary">
                <Avatar className="h-7 w-7">
                  <AvatarFallback className="bg-secondary text-xs font-medium">
                    {initials}
                  </AvatarFallback>
                </Avatar>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium leading-tight">
                    {user.name} {user.surname}
                  </p>
                  <RoleChip role={user.role} />
                </div>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" side="top" className="w-48">
              <DropdownMenuItem asChild>
                <Link href={`/users/${user.id}`}>Profile</Link>
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => logout()} variant="destructive">
                <LogOut className="h-4 w-4" />
                Sign out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      )}
    </aside>
  );
}
