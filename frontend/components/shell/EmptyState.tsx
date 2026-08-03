import type { ReactNode } from "react";

/** An empty screen is an invitation to act, not an apology. */
export function EmptyState({
  title,
  description,
  action,
  icon,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
  icon?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded border border-dashed border-rule px-6 py-12 text-center">
      {icon}
      <p className="font-heading text-sm font-semibold text-ink">{title}</p>
      {description && <p className="max-w-sm text-sm text-slate">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
