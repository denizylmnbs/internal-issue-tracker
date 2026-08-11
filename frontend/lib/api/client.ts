import { ApiClientError } from "./errors";
import type { ApiResponse } from "./types";

/**
 * The single fetch wrapper every hook and mutation goes through. Runs in the
 * browser only — it always hits our own same-origin `/bff/api/...` proxy
 * (see app/bff/[...path]/route.ts), which is the thing that actually attaches
 * the bearer token from the httpOnly cookie. That keeps the token out of any
 * code that runs in the page.
 *
 * `path` is the backend path, e.g. "/api/projects/7/issues?sprintId=4".
 */
export async function apiFetch<T>(
  path: string,
  init?: RequestInit,
): Promise<T | undefined> {
  const res = await fetch(`/bff${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (res.status === 401) {
    // The proxy already cleared the cookie for us. Bounce to login with a
    // return path so the session-expiry story is one code path, not one per
    // hook. Skip this on the auth pages themselves — SessionProvider's
    // GET /api/auth/me probe 401s there by design (no cookie yet), and a
    // hard redirect back to /login would just remount the app and refire
    // the same probe, looping forever instead of showing the form.
    const onAuthPage =
      typeof window !== "undefined" &&
      (window.location.pathname === "/login" ||
        window.location.pathname === "/register");

    if (typeof window !== "undefined" && !onAuthPage) {
      const next = encodeURIComponent(
        window.location.pathname + window.location.search,
      );
      window.location.href = `/login?next=${next}`;
    }
  }

  // DELETEs and reset-password succeed with no body at all in some setups;
  // guard against an empty response before parsing.
  const text = await res.text();
  const body = (text ? JSON.parse(text) : { success: true }) as ApiResponse<T>;

  if (!body.success) {
    const retryAfter = res.headers.get("Retry-After");
    throw new ApiClientError(
      res.status,
      body.error,
      retryAfter ? Number(retryAfter) : undefined,
    );
  }

  return body.data;
}

/** Asserts a payload is present — for endpoints that always return data. */
export async function apiData<T>(path: string, init?: RequestInit): Promise<T> {
  const data = await apiFetch<T>(path, init);
  if (data === undefined) {
    throw new Error(`Expected a response body from ${path}, got none.`);
  }
  return data;
}

/** For DELETEs and other endpoints with an intentionally empty envelope. */
export async function apiVoid(path: string, init?: RequestInit): Promise<void> {
  await apiFetch<undefined>(path, init);
}

export function json(body: unknown, method: string): RequestInit {
  return { method, body: JSON.stringify(body) };
}

/** Builds a query string from optional params, skipping null/undefined so
 * absent filters don't show up as `?foo=undefined`. */
export function toQuery(
  params: Record<string, string | number | boolean | undefined | null>,
): string {
  const usp = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      usp.set(key, String(value));
    }
  }
  const qs = usp.toString();
  return qs ? `?${qs}` : "";
}
