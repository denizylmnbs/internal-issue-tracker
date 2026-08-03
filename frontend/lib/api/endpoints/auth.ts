import type { LoginRequest, LoginResponse } from "../types";

/** Only used server-side, from app/api/session/route.ts, which talks to the
 * backend directly rather than through the /bff proxy (there is no cookie to
 * proxy with yet — this call is what creates it). */
export const login = (
  body: LoginRequest,
  apiBaseUrl: string,
): Promise<LoginResponse> =>
  fetch(`${apiBaseUrl}/api/auth/login`, {
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
    return payload.data as LoginResponse;
  });
