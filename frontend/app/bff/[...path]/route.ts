import { cookies } from "next/headers";
import { NextResponse, type NextRequest } from "next/server";

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
 */

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";
const SESSION_COOKIE = "ist_at";

async function proxy(req: NextRequest, path: string[]): Promise<Response> {
  const cookieStore = await cookies();
  const token = cookieStore.get(SESSION_COOKIE)?.value;

  // `path` is everything after /bff/ — since client.ts calls `/bff${path}`
  // where `path` already starts with "/api/...", `path` here already
  // includes the leading "api" segment. Don't prepend it again.
  const targetUrl = new URL(`/${path.join("/")}`, API_BASE_URL);
  targetUrl.search = req.nextUrl.search;

  const hasBody = !["GET", "HEAD", "DELETE"].includes(req.method);

  const upstream = await fetch(targetUrl, {
    method: req.method,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: hasBody ? await req.text() : undefined,
    cache: "no-store",
  });

  const responseBody = await upstream.text();
  const headers = new Headers({ "Content-Type": "application/json" });
  const location = upstream.headers.get("Location");
  if (location) headers.set("Location", location);

  const response = new NextResponse(responseBody, {
    status: upstream.status,
    headers,
  });

  // The backend never issues a new token itself, but a request that fails
  // authentication is the signal a stale/expired cookie is worthless — clear
  // it so the client's global 401 handler lands on a clean login rather than
  // looping.
  if (upstream.status === 401) {
    response.cookies.delete(SESSION_COOKIE);
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
