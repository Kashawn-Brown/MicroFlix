// app/lib/auth-header.tsx
"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { loadAuth, clearAuth } from "../lib/auth-storage";

type AuthState = {
  displayName: string | null;
  email: string | null;
  token: string | null;
};

export default function AuthHeader() {
  const [auth, setAuth] = useState<AuthState | null>(null);

  useEffect(() => {
    const stored = loadAuth();
    if (!stored || !stored.token) {
      setAuth(null);
    } else {
      setAuth({
        displayName: stored.displayName ?? null,
        email: stored.email ?? null,
        token: stored.token,
      });
    }
  }, []);

  function handleLogout() {
    clearAuth();
    setAuth(null);
    // For now, a simple hard reload is fine to refresh UI everywhere.
    window.location.href = "/";
  }

  if (!auth) {
    return (
      <div className="flex items-center gap-3 text-xs">
        <Link
          href="/login"
          className="rounded-md bg-sky-500 px-3 py-1 text-xs font-medium text-slate-950 hover:bg-sky-400"
        >
          Login
        </Link>
        <Link
          href="/login?mode=register"
          className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-sky-400 hover:text-sky-200"
        >
          Create account
        </Link>
      </div>
    );
  }

  const label = auth.displayName || auth.email || "You";

  return (
    <div className="flex items-center gap-3 text-xs">
      <span className="text-slate-300">
        Signed in as{" "}
        <Link
          href="/profile"
          className="font-medium text-sky-300 hover:text-sky-200"
        >
          {label}
        </Link>
      </span>
      <button
        type="button"
        onClick={handleLogout}
        className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-red-500 hover:text-red-200"
      >
        Logout
      </button>
    </div>
  );
}
