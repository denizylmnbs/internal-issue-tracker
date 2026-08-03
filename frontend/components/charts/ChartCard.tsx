import type { ReactNode } from "react";

/** One shell for every metric block — a label, the resolved window/asOf
 * beside it (docs/API.md §4.13: "a metric without its window is not a
 * metric"), and the chart or figure itself. */
export function ChartCard({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
}) {
  return (
    <div className="rounded border border-rule p-4">
      <div className="mb-3 flex items-baseline justify-between">
        <h3 className="font-heading text-sm font-semibold">{title}</h3>
        {subtitle && <span className="font-data text-[11px] text-slate">{subtitle}</span>}
      </div>
      {children}
    </div>
  );
}

export function StatFigure({ value, label }: { value: string; label: string }) {
  return (
    <div>
      <p className="font-data text-2xl font-semibold leading-none">{value}</p>
      <p className="mt-1 text-xs text-slate">{label}</p>
    </div>
  );
}
