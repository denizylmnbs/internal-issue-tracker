import { cookies } from "next/headers";
import { NextResponse, type NextRequest } from "next/server";
import {
  ACCESS_COOKIE,
  REFRESH_COOKIE,
  accessCookieOptions,
  refreshCookieOptions,
} from "@/lib/auth/cookies";
import { refreshSession } from "@/lib/auth/refresh";

/**
 * The BFF proxy. Every browser call goes through here rather than straight to
 * the Spring backend: it reads the httpOnly session cookie, attaches it as an
 * `Authorization: Bearer` header, and forwards the request. The token never
 * reaches page JavaScript, and the browser never needs to know the backend's
 * origin — which also means the browser-side CORS story is a non-issue on
 * this path (see docs/API.md §6.1; it's still configured for direct calls).
 *
 * This is a raw byte-level proxy, not envelope-aware: it doesn't parse the
 * `ApiResponse<T>` body, it just relays it. Unwrapping happens client-side in
 * lib/api/client.ts.
 *
 * A 401 from the backend means the access token is missing/expired/invalid
 * (docs/API.md — the JWT filter never 401s itself, authorization does). If a
 * refresh cookie is present we spend it on one `/api/auth/refresh` attempt
 * (coalesced via lib/auth/refresh.ts so concurrent requests don't race the
 * same single-use token) and replay the original request exactly once with
 * the new access token. Only if that also fails do we give up, clear both
 * cookies, and let the original 401 through — lib/api/client.ts's global
 * handler then bounces to /login.
 */

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

async function proxy(req: NextRequest, path: string[]): Promise<Response> {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get(ACCESS_COOKIE)?.value;
  const refreshToken = cookieStore.get(REFRESH_COOKIE)?.value;

  // `path` is everything after /bff/ — since client.ts calls `/bff${path}`
  // where `path` already starts with "/api/...", `path` here already
  // includes the leading "api" segment. Don't prepend it again.
  const targetUrl = new URL(`/${path.join("/")}`, API_BASE_URL);
  targetUrl.search = req.nextUrl.search;

  const hasBody = !["GET", "HEAD", "DELETE"].includes(req.method);
  // Read the body once up front — it can't be read again for the retry after a refresh, the
  // stream is consumed the first time (also why this can't stream the body through instead).
  // Multipart uploads (avatar upload) carry a boundary parameter inside their own Content-Type,
  // so that header is forwarded verbatim below rather than hard-coded to JSON; buffering as raw
  // bytes rather than text keeps a binary body intact either way. Route Handlers have no built-in
  // body-size limit the way the old Pages API routes did, so the multipart size cap is enforced by
  // Spring (spring.servlet.multipart.max-file-size), not here.
  const requestContentType = req.headers.get("content-type");
  const body = hasBody ? Buffer.from(await req.arrayBuffer()) : undefined;

  const forward = (token: string | undefined) =>
    fetch(targetUrl, {
      method: req.method,
      headers: {
        "Content-Type": requestContentType ?? "application/json",
        Accept: "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body,
      cache: "no-store",
    });

  let upstream = await forward(accessToken);
  let newPair: { accessToken: string; refreshToken: string } | undefined;

  if (upstream.status === 401 && refreshToken) {
    try {
      newPair = await refreshSession(refreshToken, API_BASE_URL);
      upstream = await forward(newPair.accessToken);
    } catch {
      // Refresh token invalid/expired/already used (INVALID_REFRESH_TOKEN)
      // or the backend is unreachable — fall through with the original 401.
    }
  }

  const responseBody = await upstream.text();
  const headers = new Headers({ "Content-Type": "application/json" });
  const location = upstream.headers.get("Location");
  if (location) headers.set("Location", location);
  const retryAfter = upstream.headers.get("Retry-After");
  if (retryAfter) headers.set("Retry-After", retryAfter);

  const response = new NextResponse(responseBody, {
    status: upstream.status,
    headers,
  });

  if (newPair) {
    // Refresh tokens rotate on every exchange — the old ist_rt is already
    // dead server-side, so the new one must always be stored.
    response.cookies.set(ACCESS_COOKIE, newPair.accessToken, accessCookieOptions);
    response.cookies.set(REFRESH_COOKIE, newPair.refreshToken, refreshCookieOptions);
  } else if (upstream.status === 401) {
    // Either there was no refresh token to try, or the refresh attempt
    // itself failed — either way the session is unrecoverable here.
    response.cookies.delete(ACCESS_COOKIE);
    response.cookies.delete(REFRESH_COOKIE);
  }

  return response;
}

type Ctx = { params: Promise<{ path: string[] }> };

export async function GET(req: NextRequest, ctx: Ctx) {
  return proxy(req, (await ctx.params).path);
}
export async function POST(req: NextRequest, ctx: Ctx) {
  return proxy(req, (await ctx.params).path);
}
export async function PUT(req: NextRequest, ctx: Ctx) {
  return proxy(req, (await ctx.params).path);
}
export async function PATCH(req: NextRequest, ctx: Ctx) {
  return proxy(req, (await ctx.params).path);
}
export async function DELETE(req: NextRequest, ctx: Ctx) {
  return proxy(req, (await ctx.params).path);
}
