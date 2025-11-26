// Client-side login page: handles form state, calls auth API, stores token, and redirects on success

"use client";  // component runs in the browser

import { FormEvent, useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { login } from "../../lib/auth-api";
import { ApiError } from "../../lib/api-client";
import { loadAuth, saveAuth } from "../../lib/auth-storage";


export default function LoginPage() {
  const router = useRouter();

  // Simple local state for form + UI feedback
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [checkingAuth, setCheckingAuth] = useState(true);

  useEffect(() => {
    const stored = loadAuth();

    if (stored && stored.token) {
      // Already signed in – send them to movies instead of showing login form.
      router.push("/movies");
      return;
    }

    setCheckingAuth(false);
  }, [router]);


  // Runs when user hits "Sign in"
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();  // prevent from reloading page
    
    // Reset and mark as submitting
    setErrorMessage(null);
    setSubmitting(true);

    try {

      // Call backend via login function in auth-api sending in user inputs for email and password
      const auth = await login({ email, password });  // If successful, get back authResponse
      
      // Save JWT + user info for later use
      saveAuth(auth);
      
      // Redirect to movies page after successful login
      router.push("/movies");
    
    } catch (error) {
      if (error instanceof ApiError) {
        
        // Prefer backend ProblemDetail message if available
        const detail =
          error.problem?.detail || error.problem?.title || "Login failed.";
        setErrorMessage(detail);
      } else {
        setErrorMessage("Something went wrong. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

   if (checkingAuth) {
    return (
      <section className="mx-auto w-full max-w-md">
        <h1 className="text-2xl font-semibold tracking-tight">Sign in</h1>
        <p className="mt-2 text-sm text-slate-300">
          Checking your session…
        </p>
      </section>
    );
  }

  return (
    <section className="mx-auto flex w-full max-w-md flex-col gap-6">
      <div className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Sign in</h1>
        <p className="text-sm text-slate-300">
          Use your MicroFlix account to rate movies and manage your watchlist.
        </p>
      </div>

      {errorMessage && (
        <div className="rounded-md border border-red-500 bg-red-950/40 px-3 py-2 text-xs text-red-200">
          {errorMessage}
        </div>
      )}

      <form className="space-y-4" onSubmit={handleSubmit}>
        <div className="space-y-1">
          <label
            htmlFor="email"
            className="block text-sm font-medium text-slate-200"
          >
            Email
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <div className="space-y-1">
          <label
            htmlFor="password"
            className="block text-sm font-medium text-slate-200"
          >
            Password
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-sky-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400 disabled:cursor-not-allowed disabled:opacity-70"
        >
          {submitting ? "Signing in..." : "Sign in"}
        </button>
      </form>

      <p className="text-xs text-slate-400">
        Don&apos;t have an account?{" "}
        <Link href="/register" className="text-sky-300 hover:text-sky-200">
          Create one
        </Link>
        .
      </p>
    </section>
  );
}
