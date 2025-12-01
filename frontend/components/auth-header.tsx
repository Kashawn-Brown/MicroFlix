// app/lib/auth-header.tsx
"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { loadAuth, clearAuth } from "../lib/auth-storage";

type AuthState = {
  displayName: string | null;
  email: string | null;
  token: string | null;
};

export default function AuthHeader() {
  const router = useRouter();
  const [auth, setAuth] = useState<AuthState | null>(null);

  useEffect(() => {
    
    // helper to pull latest auth from localStorage
    function syncFromStorage() {
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
    }

    // initial load
    syncFromStorage();

    // listen for global "auth changed" events
    if (typeof window !== "undefined") {
      window.addEventListener("microflix-auth-changed", syncFromStorage);
      return () => {
        window.removeEventListener("microflix-auth-changed", syncFromStorage);
      };
    }

  }, []);

  function handleLogout() {
    clearAuth();
    setAuth(null);
    router.push("/login");
  }
  console.log("auth: " + auth)

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
          href="/register"
          className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-sky-400 hover:text-sky-200"
        >
          Create account
        </Link>
      </div>
    );
  }

  const label = auth.displayName || auth.email || "You";

  return (
    <div className="flex items-center gap-3 text">
      <span className="text-slate-300">
        Welcome, {" "}
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
