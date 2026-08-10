import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE, REFRESH_COOKIE } from "@/lib/auth/cookies";

/**
 * Route gate for the authenticated shell. Next.js 16 renamed the
 * `middleware.ts` convention to `proxy.ts` (the exported function is now
 * named `proxy`, not `middleware`) — see the version-16 upgrade guide.
 *
 * This only checks that *some* session cookie is present — access or
 * refresh. Validity is the backend's call: an expired/tampered access token
 * still passes this gate and gets caught by the 401 handling in
 * app/bff/[...path]/route.ts, which tries a refresh before clearing cookies
 * and redirecting. Gating on ist_at alone would bounce a user to /login the
 * moment the (short-lived) access token expires, even with a perfectly good
 * refresh token still in hand — so a live ist_rt also counts as "has session".
 */

export function proxy(request: NextRequest) {
  const hasSession =
    request.cookies.has(ACCESS_COOKIE) || request.cookies.has(REFRESH_COOKIE);

  if (!hasSession) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set(
      "next",
      request.nextUrl.pathname + request.nextUrl.search,
    );
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  // An explicit allow-list of protected prefixes rather than a negative
  // lookahead: this Next.js version's path-to-regexp does not support
  // `(?!...)` groups — it throws "Unexpected ( at index 1" — so a
  // lookahead-based matcher silently fails to exclude anything, and proxy
  // ends up running on /login itself, redirecting it to /login?next=/login
  // and compounding on every hit. Verified directly against `path-to-regexp`
  // before landing on this shape.
  matcher: [
    "/",
    "/projects",
    "/projects/:path*",
    "/teams",
    "/teams/:path*",
    "/users/:path*",
    "/admin/:path*",
  ],
};
