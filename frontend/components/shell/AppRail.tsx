"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutGrid, FolderKanban, Users, ShieldCheck, LogOut, Tags } from "lucide-react";
import { cn } from "@/lib/utils";
import { useSession } from "@/lib/auth/session";
import { isAdmin } from "@/lib/auth/can";
import { useRailCollapsed } from "@/lib/hooks/useRailCollapsed";
import { RoleChip } from "./chips";
import { RailLink, RailToggle } from "./rail";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { UserAvatar } from "./UserAvatar";

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
  const [collapsed, toggle] = useRailCollapsed("app");

  return (
    <aside
      className={cn(
        "flex h-full shrink-0 flex-col border-r border-rule bg-sidebar transition-[width] duration-150",
        collapsed ? "w-14" : "w-56",
      )}
    >
      <div className="flex h-12 items-center border-b border-rule">
        <Link
          href="/"
          aria-label="Issue Tracker home"
          className={cn(
            "flex h-full min-w-0 items-center gap-2 hover:bg-secondary",
            collapsed ? "w-full justify-center" : "flex-1 px-4",
          )}
        >
          <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded bg-signal font-heading text-xs font-bold text-signal-foreground">
            I
          </div>
          {!collapsed && (
            <span className="truncate font-heading text-sm font-semibold tracking-tight">
              Issue Tracker
            </span>
          )}
        </Link>
        {!collapsed && <RailToggle collapsed={collapsed} onToggle={toggle} what="menu" className="mr-2" />}
      </div>

      {collapsed && (
        <div className="flex justify-center border-b border-rule py-1.5">
          <RailToggle collapsed={collapsed} onToggle={toggle} what="menu" />
        </div>
      )}

      <nav className="flex-1 space-y-0.5 px-2 py-3">
        {NAV.map(({ href, label, icon, exact }) => (
          <RailLink
            key={href}
            href={href}
            label={label}
            icon={icon}
            collapsed={collapsed}
            active={exact ? pathname === href : pathname.startsWith(href)}
          />
        ))}
        {isAdmin(user) && (
          <>
            {collapsed ? (
              <div className="mx-2 mt-3 mb-1 border-t border-rule" aria-hidden />
            ) : (
              <p className="mt-3 px-2.5 text-xs font-medium uppercase tracking-wide text-slate">
                Admin
              </p>
            )}
            {ADMIN_NAV.map(({ href, label, icon }) => (
              <RailLink
                key={href}
                href={href}
                label={label}
                icon={icon}
                collapsed={collapsed}
                active={pathname.startsWith(href)}
              />
            ))}
          </>
        )}
      </nav>

      {user && (
        <div className="border-t border-rule p-2">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                aria-label={collapsed ? `${user.name} ${user.surname}` : undefined}
                className={cn(
                  "flex w-full items-center rounded py-2 text-left hover:bg-secondary",
                  collapsed ? "justify-center px-0" : "gap-2.5 px-2",
                )}
              >
                <UserAvatar name={user.name} surname={user.surname} avatarUrl={user.avatarUrl} />
                {!collapsed && (
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium leading-tight">
                      {user.name} {user.surname}
                    </p>
                    <RoleChip role={user.role} />
                  </div>
                )}
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
