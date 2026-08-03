/**
 * Chart palette mirrors app/globals.css's tokens exactly (light mode values —
 * Recharts renders SVG fills that don't reliably resolve CSS custom
 * properties across all export/print paths, so these are the same hexes
 * pinned as literals). `signal` is the categorical default; `moss`/`amber`/
 * `rust` are reserved for the same good/warning/critical semantics they
 * carry everywhere else in the app — never reused as "series 4".
 */
export const CHART = {
  signal: "#3B4CD6",
  moss: "#2F7A4F",
  amber: "#C2760B",
  rust: "#B23A2E",
  slate: "#5A6478",
  ink: "#12151C",
  rule: "#E3E6ED",
  purple: "#7C5CD6",
} as const;

/** Fixed categorical order — never cycled arbitrarily per chart. */
export const CATEGORICAL = [CHART.signal, CHART.moss, CHART.amber, CHART.rust, CHART.purple, CHART.slate];

export const TOOLTIP_STYLE = {
  fontSize: 12,
  fontFamily: "var(--font-plex-sans)",
  background: "var(--paper)",
  border: `1px solid ${CHART.rule}`,
  borderRadius: 4,
  boxShadow: "0 2px 8px rgba(0,0,0,0.08)",
};

export const AXIS_STYLE = { fontSize: 11, fill: CHART.slate, fontFamily: "var(--font-plex-mono)" };
