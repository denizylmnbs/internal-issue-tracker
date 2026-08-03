"use client";

import { useQuery } from "@tanstack/react-query";
import { listTeams } from "@/lib/api/endpoints/teams";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function TeamPicker({
  value,
  onChange,
  placeholder = "No team",
  disabled,
}: {
  value: number | null;
  onChange: (id: number | null) => void;
  placeholder?: string;
  disabled?: boolean;
}) {
  const { data } = useQuery({
    queryKey: ["teams", "picker"],
    queryFn: () => listTeams({ size: 200, sort: "name,asc" }),
  });

  return (
    <Select
      value={value != null ? String(value) : "NONE"}
      onValueChange={(v) => onChange(v === "NONE" ? null : Number(v))}
      disabled={disabled}
    >
      <SelectTrigger className="w-full">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="NONE">{placeholder}</SelectItem>
        {data?.content.filter((t) => t.isActive).map((t) => (
          <SelectItem key={t.id} value={String(t.id)}>
            {t.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
