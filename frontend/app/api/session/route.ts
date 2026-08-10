import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { login, logout } from "@/lib/api/endpoints/auth";
import {
  ACCESS_COOKIE,
  REFRESH_COOKIE,
  accessCookieOptions,
  refreshCookieOptions,
} from "@/lib/auth/cookies";

/**
 * Creates and destroys the session cookies. This is the *only* place that
 * calls the backend's `/api/auth/login` and `/api/auth/logout` directly —
 * before login succeeds there is no cookie yet for the /bff proxy to attach,
 * and logout needs to revoke the refresh token server-side before it's gone.
 */

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export async function POST(req: Request) {
  const body = await req.json();

  try {
    const { accessToken, refreshToken } = await login(body, API_BASE_URL);

    const cookieStore = await cookies();
    cookieStore.set(ACCESS_COOKIE, accessToken, accessCookieOptions);
    cookieStore.set(REFRESH_COOKIE, refreshToken, refreshCookieOptions);

    return NextResponse.json({ success: true });
  } catch (reason) {
    const err = reason as { status?: number; code?: string; message?: string };
    return NextResponse.json(
      {
        success: false,
        error: { code: err.code ?? "INTERNAL_ERROR", message: err.message ?? "Login failed." },
      },
      { status: err.status ?? 500 },
    );
  }
}

export async function DELETE() {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(REFRESH_COOKIE)?.value;

  if (refreshToken) {
    // Best-effort: logout is idempotent server-side (an already-invalid
    // token is accepted silently), and a backend that's unreachable
    // shouldn't leave the user stuck logged in on the client.
    try {
      await logout(refreshToken, API_BASE_URL);
    } catch {
      // Ignore — cookies are cleared below regardless.
    }
  }

  cookieStore.delete(ACCESS_COOKIE);
  cookieStore.delete(REFRESH_COOKIE);
  return NextResponse.json({ success: true });
}
