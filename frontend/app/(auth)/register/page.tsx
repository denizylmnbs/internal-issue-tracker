"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { register as registerUser } from "@/lib/api/endpoints/users";
import { isApiClientError } from "@/lib/api/errors";
import { SESSION_QUERY_KEY } from "@/lib/auth/session";

const schema = z.object({
  name: z.string().min(2, "At least 2 characters").max(255),
  surname: z.string().min(2, "At least 2 characters").max(255),
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  password: z.string().min(8, "At least 8 characters"),
});
type FormValues = z.infer<typeof schema>;

export default function RegisterPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register: field,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    try {
      // register() hits /bff, which needs no cookie — the route itself is
      // public — but we don't have one yet either way. Call the same client
      // path (docs/API.md §4.2: no auth required on this route).
      await registerUser(values);
    } catch (err) {
      setServerError(
        isApiClientError(err) ? err.message : "Registration failed. Try again.",
      );
      return;
    }

    // Registration always makes a plain USER, no token — sign in next.
    const res = await fetch("/api/session", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: values.email, password: values.password }),
    });
    const body = await res.json();
    if (!body.success) {
      router.push("/login");
      return;
    }
    await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY });
    router.push("/");
    router.refresh();
  };

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-4 rounded border border-rule bg-card p-6"
    >
      <div>
        <h1 className="font-heading text-xl font-semibold tracking-tight">
          Create an account
        </h1>
        <p className="mt-1 text-sm text-slate">
          You'll start as a read-only User — someone with Editor or above can
          promote you once you're on a team.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="name">First name</Label>
          <Input id="name" {...field("name")} />
          {errors.name && <p className="text-xs text-rust">{errors.name.message}</p>}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="surname">Last name</Label>
          <Input id="surname" {...field("surname")} />
          {errors.surname && <p className="text-xs text-rust">{errors.surname.message}</p>}
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="email">Email</Label>
        <Input id="email" type="email" autoComplete="email" {...field("email")} />
        {errors.email && <p className="text-xs text-rust">{errors.email.message}</p>}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="password">Password</Label>
        <Input
          id="password"
          type="password"
          autoComplete="new-password"
          {...field("password")}
        />
        {errors.password && <p className="text-xs text-rust">{errors.password.message}</p>}
      </div>

      {serverError && <p className="text-sm text-rust">{serverError}</p>}

      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? "Creating account…" : "Create account"}
      </Button>

      <p className="text-center text-sm text-slate">
        Already have an account?{" "}
        <Link href="/login" className="text-signal hover:underline">
          Sign in
        </Link>
      </p>
    </form>
  );
}
