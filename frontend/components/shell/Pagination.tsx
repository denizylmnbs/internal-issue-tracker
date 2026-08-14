"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

/**
 * Prev/next over a zero-based page index, for both kinds of paging in the app:
 * a slice of an in-memory list (project settings) and a `PagedResponse` whose
 * `page.totalPages` the server worked out (the activity feed).
 *
 * Renders nothing at one page or fewer, so a caller can hand it any count
 * without guarding first. `total`, when given, labels what is being paged -
 * "142 events" - which the settings lists do not need but a feed with no other
 * sense of size does.
 */
export function Pagination({
  page,
  pageCount,
  onChange,
  total,
  className,
}: {
  page: number;
  pageCount: number;
  onChange: (page: number) => void;
  total?: string;
  className?: string;
}) {
  if (pageCount <= 1) return null;

  return (
    <div className={className ?? "mt-2 flex items-center justify-between"}>
      <p className="text-xs text-slate">
        Page {page + 1} of {pageCount}
        {total && ` · ${total}`}
      </p>
      <div className="flex gap-1">
        <Button
          variant="outline"
          size="icon"
          className="h-7 w-7"
          aria-label="Previous page"
          disabled={page === 0}
          onClick={() => onChange(page - 1)}
        >
          <ChevronLeft className="h-3.5 w-3.5" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          className="h-7 w-7"
          aria-label="Next page"
          disabled={page >= pageCount - 1}
          onClick={() => onChange(page + 1)}
        >
          <ChevronRight className="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  );
}
