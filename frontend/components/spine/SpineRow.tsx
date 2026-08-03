import { cn } from "@/lib/utils";
import { formatClock, formatDateOnly } from "@/lib/format";

/**
 * The primitive behind the signature "activity spine": a mono timestamp, a
 * tick on a continuous 1px rule, and a content slot. Used identically on the
 * issue page (interleaving comments and activity), the project activity
 * feed, and — with the same tick grammar as the chart x-axis — the insights
 * page. Everything else in the design stays quiet so this is the thing that
 * reads as the product's own idea rather than a template's.
 */
export function SpineRow({
  timestamp,
  dotClassName,
  children,
  showDate,
}: {
  timestamp: string;
  dotClassName?: string;
  children: React.ReactNode;
  /** First row of a new day gets the date instead of just the clock. */
  showDate?: boolean;
}) {
  return (
    <div className="group relative flex gap-3 pb-4 pl-1 last:pb-0">
      <div className="flex w-14 shrink-0 flex-col items-end pt-0.5 text-right">
        <span className="font-data text-[11px] text-slate">
          {showDate ? formatDateOnly(timestamp) : formatClock(timestamp)}
        </span>
      </div>
      <div className="relative flex shrink-0 flex-col items-center">
        <span
          className={cn(
            "z-10 mt-1 h-2 w-2 shrink-0 rounded-full ring-4 ring-paper",
            dotClassName ?? "bg-slate",
          )}
        />
        <span className="w-px flex-1 bg-rule group-last:hidden" />
      </div>
      <div className="min-w-0 flex-1 pb-1">{children}</div>
    </div>
  );
}
