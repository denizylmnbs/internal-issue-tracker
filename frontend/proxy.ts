import { NextResponse, type NextRequest } from "next/server";

/**
 * Route gate for the authenticated shell. Next.js 16 renamed the
 * `middleware.ts` convention to `proxy.ts` (the exported function is now
 * named `proxy`, not `middleware`) — see the version-16 upgrade guide.
 *
 * This only checks the session cookie's *presence*. Validity is the
 * backend's call: an expired or tampered token still passes this gate and
 * gets caught by the 401 handling in app/bff/[...path]/route.ts and
 * lib/api/client.ts, which clear the cookie and redirect.
 */
const SESSION_COOKIE = "ist_at";

export function proxy(request: NextRequest) {
  const hasSession = request.cookies.has(SESSION_COOKIE);

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
