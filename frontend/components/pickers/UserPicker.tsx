"use client";

import { useState } from "react";
import { Check, ChevronsUpDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { useUserDirectory } from "@/lib/users/directory";
import { isEligibleForMembership } from "@/lib/auth/can";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";

/**
 * A searchable person picker over the client-cached user directory
 * (lib/users/directory.tsx). `eligibleOnly` filters out plain USERs — the
 * membership rule from docs/API.md §3: adding someone below DEVELOPER yields
 * 403 USER_ROLE_NOT_ENOUGH, so the picker just doesn't offer them, with a
 * note explaining why rather than silently omitting.
 */
export function UserPicker({
  value,
  onChange,
  eligibleOnly,
  placeholder = "Select a person…",
  disabled,
}: {
  value: number | null;
  onChange: (id: number | null) => void;
  eligibleOnly?: boolean;
  placeholder?: string;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const { byId } = useUserDirectory();
  const all = Array.from(byId.values()).filter((u) => u.isActive);
  const eligible = eligibleOnly ? all.filter((u) => isEligibleForMembership(u.role)) : all;
  const selected = value != null ? byId.get(value) : undefined;

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          disabled={disabled}
          className="w-full justify-between font-normal"
        >
          {selected ? `${selected.name} ${selected.surname}` : placeholder}
          <ChevronsUpDown className="h-3.5 w-3.5 shrink-0 text-slate" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-72 p-0" align="start">
        <Command>
          <CommandInput placeholder="Search people…" />
          <CommandList>
            <CommandEmpty>
              {eligibleOnly ? "No Developer-or-above users match." : "No one matches."}
            </CommandEmpty>
            <CommandGroup>
              {eligible.map((u) => (
                <CommandItem
                  key={u.id}
                  value={`${u.name} ${u.surname} ${u.email}`}
                  onSelect={() => {
                    onChange(u.id);
                    setOpen(false);
                  }}
                >
                  <Check className={cn("h-4 w-4", value === u.id ? "opacity-100" : "opacity-0")} />
                  <span>
                    {u.name} {u.surname}
                  </span>
                  <span className="ml-auto text-xs text-slate">{u.role}</span>
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
