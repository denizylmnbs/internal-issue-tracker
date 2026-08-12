import { refresh } from "@/lib/api/endpoints/auth";
import type { LoginResponse } from "@/lib/api/types";

/**
 * Coalesces concurrent refresh attempts that share the same (single-use)
 * refresh token, called from app/bff/[...path]/route.ts whenever a proxied
 * request 401s. Refresh tokens are consumed on exchange (Redis GETDEL,
 * docs/API.md §6.3) — without this, two requests racing on an expired access
 * token would both spend the same refresh token; the loser gets
 * INVALID_REFRESH_TOKEN and the user is bounced to login for no reason.
 *
 * `inflight` shares the in-progress exchange across requests that arrive
 * while it's still running. `recent` remembers the result for a short window
 * after it lands, for the request that arrives *after* the exchange
 * completed but *before* the browser resent the new ist_rt cookie — it would
 * otherwise show up with the now-dead old token instead of the new one.
 *
 * This only coalesces within a single Node process/instance. In a
 * multi-instance deployment two instances can still race each other; the
 * loser there just falls through to the normal 401 -> login flow (no data is
 * lost, the user just has to sign in again).
 */

const RECENT_TTL_MS = 30_000;

const inflight = new Map<string, Promise<LoginResponse>>();
const recent = new Map<string, { pair: LoginResponse; at: number }>();

function pruneRecent(): void {
  const cutoff = Date.now() - RECENT_TTL_MS;
  for (const [token, entry] of recent) {
    if (entry.at < cutoff) recent.delete(token);
  }
}

export async function refreshSession(
  oldToken: string,
  apiBaseUrl: string,
): Promise<LoginResponse> {
  pruneRecent();

  const cached = recent.get(oldToken);
  if (cached) return cached.pair;

  const existing = inflight.get(oldToken);
  if (existing) return existing;

  const attempt = refresh(oldToken, apiBaseUrl)
    .then((pair) => {
      recent.set(oldToken, { pair, at: Date.now() });
      return pair;
    })
    .finally(() => {
      inflight.delete(oldToken);
    });

  inflight.set(oldToken, attempt);
  return attempt;
}
