"use client";

import { Button } from "@/components/ui/button";

/**
 * Safety net for this route segment (board, insights, activity, settings…).
 * Before this existed a single uncaught render error anywhere under
 * /projects/{id} — e.g. the Insights date inputs producing a `RangeError`
 * from an incomplete date — fell all the way through to Next's own generic
 * "This page couldn't load" screen with no way back except a hard reload.
 * `reset()` re-renders the segment without a full navigation, which is
 * enough for errors like that one where the bad input is gone by the next
 * render.
 */
export default function ProjectSegmentError({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-3 p-6 text-center">
      <p className="font-heading text-sm font-semibold">Something went wrong</p>
      <p className="max-w-sm text-sm text-slate">
        This part of the page hit an error. Try again — if it keeps happening, refresh the page.
      </p>
      <Button size="sm" variant="outline" onClick={() => reset()}>
        Try again
      </Button>
    </div>
  );
}
