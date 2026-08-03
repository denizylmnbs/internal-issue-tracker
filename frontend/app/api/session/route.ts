import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { login } from "@/lib/api/endpoints/auth";

/**
 * Creates and destroys the session cookie. This is the *only* place that
 * calls the backend's `/api/auth/login` directly — before this call succeeds
 * there is no cookie yet for the /bff proxy to attach.
 */

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";
const SESSION_COOKIE = "ist_at";
const JWT_EXPIRATION_MS = Number(process.env.JWT_EXPIRATION_MS ?? 3_600_000);

export async function POST(req: Request) {
  const body = await req.json();

  try {
    const { accessToken } = await login(body, API_BASE_URL);

    const cookieStore = await cookies();
    cookieStore.set(SESSION_COOKIE, accessToken, {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: Math.floor(JWT_EXPIRATION_MS / 1000),
    });

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
  cookieStore.delete(SESSION_COOKIE);
  return NextResponse.json({ success: true });
}
