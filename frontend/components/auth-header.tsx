// Auth header widget: syncs with localStorage auth and shows login/register or "signed in as X" + logout

"use client";  // this runs in the browser

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { clearAuth, loadAuth } from "../lib/auth-storage";

type AuthHeaderState = {
  loading: boolean;
  displayName: string | null;
  authenticated: boolean;
};

/**
 * Small header fragment that shows auth state:
 * - "Login" link when signed out
 * - "Signed in as X · Logout" when signed in
 */
export default function AuthHeader() {
  const router = useRouter();

  // Initial states
  const [state, setState] = useState<AuthHeaderState>({
    loading: true,
    displayName: null,
    authenticated: false,
  });

  // Sync with localStorage + listen for changes
  useEffect(() => {

    // Functions to look at localStorage and decide what to show
    function syncFromStorage() {
      const stored = loadAuth();
    
      if (!stored || !stored.token) {
        setState({
          loading: false,
          displayName: null,
          authenticated: false,
        });
        return;
      }

      setState({
        loading: false,
        displayName: stored.displayName ?? null,
        authenticated: true,
      });

    }
    
    // Run once on mount
    syncFromStorage();

    // Listen for future auth changes
    if (typeof window !== "undefined") {
      window.addEventListener("microflix-auth-changed", syncFromStorage);
    }

    return () => {
      if (typeof window !== "undefined") {
        window.removeEventListener("microflix-auth-changed", syncFromStorage);
      }
    };
    
  }, []);

  // Handler for user logout
  function handleLogout() {
    clearAuth();
    // Simple redirect after logout.
    router.push("/login");
  }

  if (state.loading) {
    return (
      <div className="text-xs text-slate-400">
        Checking session…
      </div>
    );
  }

  // What to show when user is not authenticated
  if (!state.authenticated) {
    return (
      <div className="flex items-center gap-3 text-xs">
        <Link
          href="/login"
          className="text-sky-300 hover:text-sky-200"
        >
          Login
        </Link>
        <span className="text-slate-500">/</span>
        <Link
          href="/register"
          className="text-sky-300 hover:text-sky-200"
        >
          Register
        </Link>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-3 text-xs text-slate-300">
      <span>
        Signed in as{" "}
        <span className="font-medium text-slate-100">
          {state.displayName ?? "you"}
        </span>
      </span>
      <button
        type="button"
        onClick={handleLogout}
        className="rounded-md border border-slate-600 px-2 py-1 text-[11px] font-medium text-slate-200 hover:border-red-500 hover:text-red-200"
      >
        Logout
      </button>
    </div>
  );
}
