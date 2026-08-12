import type { FieldDefinitionResponse } from "@/lib/api/types";

/**
 * `field_definitions.color` is nullable, and the migration that seeded the
 * built-in codes (BACKLOG, DONE, BUG, …) left every one of them null — a
 * project only gets a hand-picked color once someone sets it from the
 * management UI. Everything that renders a status/type/priority chip needs a
 * color regardless, so a fixed, visually-distinct palette is cycled through
 * by a stable key (the code, or the id as a fallback) when none is set.
 *
 * Chosen to read reasonably against both light and dark surfaces without
 * per-theme branching — mid-saturation, mid-lightness tones.
 */
const FALLBACK_PALETTE = [
  "#64748B", // slate
  "#2563EB", // blue
  "#16A34A", // green
  "#D97706", // amber
  "#DC2626", // red
  "#7C3AED", // violet
  "#0D9488", // teal
  "#DB2777", // pink
];

function hashString(value: string): number {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

/** A code with no matching field definition (e.g. a soft-deleted one still
 * referenced by old activity data) still gets a stable color, keyed on the
 * raw string alone. */
export function fallbackColorFor(key: string): string {
  return FALLBACK_PALETTE[hashString(key) % FALLBACK_PALETTE.length];
}

/** The color to render for a field definition — its own if set, otherwise a
 * stable fallback keyed on its code. */
export function resolveColor(
  def: FieldDefinitionResponse | undefined,
  fallbackKey: string,
): string {
  return def?.color ?? fallbackColorFor(def?.code ?? fallbackKey);
}
