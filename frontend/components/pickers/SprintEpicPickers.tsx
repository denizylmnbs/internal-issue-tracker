"use client";

import { useSprintsList } from "@/lib/hooks/useSprints";
import { useEpicsList } from "@/lib/hooks/useEpics";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function SprintPicker({
  projectId,
  value,
  onChange,
}: {
  projectId: number;
  value: number | null;
  onChange: (id: number | null) => void;
}) {
  const { data } = useSprintsList(projectId, { size: 100, sort: "startDate,desc" });
  return (
    <Select
      value={value != null ? String(value) : "NONE"}
      onValueChange={(v) => onChange(v === "NONE" ? null : Number(v))}
    >
      <SelectTrigger className="w-full">
        <SelectValue placeholder="Backlog (no sprint)" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="NONE">Backlog (no sprint)</SelectItem>
        {data?.content.map((s) => (
          <SelectItem key={s.id} value={String(s.id)}>
            {s.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

export function EpicPicker({
  projectId,
  value,
  onChange,
}: {
  projectId: number;
  value: number | null;
  onChange: (id: number | null) => void;
}) {
  const { data } = useEpicsList(projectId, { size: 100 });
  return (
    <Select
      value={value != null ? String(value) : "NONE"}
      onValueChange={(v) => onChange(v === "NONE" ? null : Number(v))}
    >
      <SelectTrigger className="w-full">
        <SelectValue placeholder="No epic" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="NONE">No epic</SelectItem>
        {data?.content.map((e) => (
          <SelectItem key={e.id} value={String(e.id)}>
            {e.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
