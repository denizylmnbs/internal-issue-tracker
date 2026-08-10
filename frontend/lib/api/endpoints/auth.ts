import type { LoginRequest, LoginResponse, RefreshRequest } from "../types";

/** Shared by every server-side auth call (login/refresh/logout): POST a JSON
 * body straight at the backend, unwrap the envelope, and throw an Error
 * carrying `status`/`code` on failure. Used from app/api/session/route.ts and
 * app/bff/[...path]/route.ts — none of which have a bearer token yet (login,
 * refresh) or need one (logout takes the refresh token in the body instead),
 * so these bypass the /bff proxy and hit the backend directly. */
function postAuth<T>(
  path: string,
  body: unknown,
  apiBaseUrl: string,
): Promise<T> {
  return fetch(`${apiBaseUrl}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  }).then(async (res) => {
    const payload = await res.json();
    if (!payload.success) {
      const err = new Error(payload.error.message) as Error & {
        status: number;
        code: string;
      };
      err.status = res.status;
      err.code = payload.error.code;
      throw err;
    }
    return payload.data as T;
  });
}

export const login = (
  body: LoginRequest,
  apiBaseUrl: string,
): Promise<LoginResponse> => postAuth("/api/auth/login", body, apiBaseUrl);

/** Exchanges a refresh token for a new access/refresh pair. The token sent is
 * consumed server-side (single-use, rotated) — docs/API.md §4.1/§6.3. */
export const refresh = (
  refreshToken: string,
  apiBaseUrl: string,
): Promise<LoginResponse> =>
  postAuth<LoginResponse>(
    "/api/auth/refresh",
    { refreshToken } satisfies RefreshRequest,
    apiBaseUrl,
  );

/** Revokes a refresh token server-side. Idempotent — an already-invalid token
 * is accepted silently, and the backend returns no `data` on success. */
export const logout = (
  refreshToken: string,
  apiBaseUrl: string,
): Promise<void> =>
  postAuth<void>(
    "/api/auth/logout",
    { refreshToken } satisfies RefreshRequest,
    apiBaseUrl,
  );
