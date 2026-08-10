/**
 * Cookie names and options shared by every server-side place that sets or
 * reads the session: app/api/session/route.ts, app/bff/[...path]/route.ts,
 * and proxy.ts. Server-only (reads process.env directly, no NEXT_PUBLIC_
 * prefix) — never import this from a Client Component.
 */

export const ACCESS_COOKIE = "ist_at";
export const REFRESH_COOKIE = "ist_rt";

const JWT_EXPIRATION_MS = Number(process.env.JWT_EXPIRATION_MS ?? 3_600_000);
const JWT_REFRESH_EXPIRATION_MS = Number(
  process.env.JWT_REFRESH_EXPIRATION_MS ?? 604_800_000,
);

const base = {
  httpOnly: true,
  sameSite: "lax" as const,
  secure: process.env.NODE_ENV === "production",
  path: "/",
};

export const accessCookieOptions = {
  ...base,
  maxAge: Math.floor(JWT_EXPIRATION_MS / 1000),
};

export const refreshCookieOptions = {
  ...base,
  maxAge: Math.floor(JWT_REFRESH_EXPIRATION_MS / 1000),
};
