"use client";

import * as React from "react";
import { useRef } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";
import { EmptyState } from "./EmptyState";
import { Skeleton } from "@/components/ui/skeleton";

export type Column<T> = {
  key: string;
  header: string;
  render: (row: T) => React.ReactNode;
  className?: string;
  headerClassName?: string;
};

export type RowKey = string | number;

/** Opt-in — tables that don't pass it render exactly as they did before. */
export type Selection = {
  selectedKeys: Set<RowKey>;
  onChange: (keys: Set<RowKey>) => void;
};

/** Rows over cards, everywhere a list is dense enough to want one — the
 * backlog, admin users, team rosters. */
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  isLoading,
  emptyTitle = "Nothing here yet",
  emptyDescription,
  onRowClick,
  selection,
  rowWrapper,
}: {
  columns: Column<T>[];
  rows: T[] | undefined;
  rowKey: (row: T) => RowKey;
  isLoading?: boolean;
  emptyTitle?: string;
  emptyDescription?: string;
  onRowClick?: (row: T) => void;
  selection?: Selection;
  /** Wraps each row — the backlog uses it to hang a context menu off one. */
  rowWrapper?: (row: T, rowElement: React.ReactNode) => React.ReactNode;
}) {
  // for shift-click ranges: which row anchored the current run
  const anchorIndex = useRef<number | null>(null);

  if (!isLoading && (!rows || rows.length === 0)) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  const keys = rows?.map(rowKey) ?? [];
  const allSelected = keys.length > 0 && keys.every((k) => selection?.selectedKeys.has(k));
  const someSelected = !allSelected && keys.some((k) => selection?.selectedKeys.has(k));

  const toggleAll = () => {
    if (!selection) return;
    anchorIndex.current = null;
    selection.onChange(allSelected ? new Set() : new Set(keys));
  };

  /** Shift extends from the last row clicked, as a file list does. */
  const toggleRow = (index: number, shiftKey: boolean) => {
    if (!selection) return;
    const next = new Set(selection.selectedKeys);

    if (shiftKey && anchorIndex.current !== null) {
      const [from, to] = [anchorIndex.current, index].sort((a, b) => a - b);
      for (let i = from; i <= to; i++) next.add(keys[i]);
    } else {
      const key = keys[index];
      if (next.has(key)) next.delete(key);
      else next.add(key);
      anchorIndex.current = index;
    }

    selection.onChange(next);
  };

  const columnCount = columns.length + (selection ? 1 : 0);

  return (
    <div className="overflow-x-auto rounded border border-rule">
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            {selection && (
              <TableHead className="h-9 w-9">
                <Checkbox
                  checked={allSelected || (someSelected && "indeterminate")}
                  onCheckedChange={toggleAll}
                  aria-label="Select all rows"
                />
              </TableHead>
            )}
            {columns.map((col) => (
              <TableHead
                key={col.key}
                className={cn("h-9 text-xs font-medium text-slate", col.headerClassName)}
              >
                {col.header}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading
            ? Array.from({ length: 6 }).map((_, i) => (
                <TableRow key={i}>
                  {Array.from({ length: columnCount }).map((_unused, c) => (
                    <TableCell key={c} className="py-2.5">
                      <Skeleton className="h-4 w-full max-w-32" />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            : rows!.map((row, index) => {
                const key = keys[index];
                const selected = !!selection?.selectedKeys.has(key);

                const rowElement = (
                  <TableRow
                    key={key}
                    data-state={selected ? "selected" : undefined}
                    className={cn(onRowClick && "cursor-pointer")}
                    onClick={() => onRowClick?.(row)}
                  >
                    {selection && (
                      <TableCell
                        className="py-2.5"
                        // the row itself navigates; ticking a box must not
                        onClick={(e) => e.stopPropagation()}
                      >
                        <Checkbox
                          checked={selected}
                          onClick={(e) => toggleRow(index, e.shiftKey)}
                          aria-label={`Select row ${index + 1}`}
                        />
                      </TableCell>
                    )}
                    {columns.map((col) => (
                      <TableCell key={col.key} className={cn("py-2.5 text-sm", col.className)}>
                        {col.render(row)}
                      </TableCell>
                    ))}
                  </TableRow>
                );

                return rowWrapper ? (
                  <React.Fragment key={key}>{rowWrapper(row, rowElement)}</React.Fragment>
                ) : (
                  rowElement
                );
              })}
        </TableBody>
      </Table>
    </div>
  );
}
