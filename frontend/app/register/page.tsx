// Client-side registration page: handles sign-up form, calls auth API, stores token, and redirects on success

"use client";  // component runs in the browser

import { FormEvent, useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { register } from "../../lib/auth-api";
import { ApiError } from "../../lib/api-client";
import { loadAuth, saveAuth } from "../../lib/auth-storage";


export default function RegisterPage() {
  const router = useRouter();

  const [displayName, setDisplayName] = useState("");
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

  // Runs when user hits "Create Account"
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();  // prevent from reloading page

    setErrorMessage(null);
    setSubmitting(true);

    try {

      // Call backend via register function in auth-api
      const auth = await register({ email, password, displayName });

      // If successful, get back authResponse + save to loacal storage
      saveAuth(auth);
      
      router.push("/movies");
      
    } catch (error) {
      if (error instanceof ApiError) {

        const detail =
          error.problem?.detail || error.problem?.title || "Registration failed.";
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
        <h1 className="text-2xl font-semibold tracking-tight">Create account</h1>
        <p className="mt-2 text-sm text-slate-300">
          Checking your session…
        </p>
      </section>
    );
  }

  return (
    <section className="mx-auto flex w-full max-w-md flex-col gap-6">
      <div className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">
          Create your account
        </h1>
        <p className="text-sm text-slate-300">
          Sign up to start rating movies and building your watchlist.
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
            htmlFor="displayName"
            className="block text-sm font-medium text-slate-200"
          >
            Display name
          </label>
          <input
            id="displayName"
            type="text"
            required
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

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
            autoComplete="new-password"
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
          {submitting ? "Creating account..." : "Create account"}
        </button>
      </form>

      <p className="text-xs text-slate-400">
        Already have an account?{" "}
        <Link href="/login" className="text-sky-300 hover:text-sky-200">
          Sign in
        </Link>
        .
      </p>
    </section>
  );
}
