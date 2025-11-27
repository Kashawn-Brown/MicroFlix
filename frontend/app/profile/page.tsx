// app/profile/page.tsx
"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { loadAuth, clearAuth } from "../../lib/auth-storage";
import { fetchMyRatings } from "../../lib/rating-api";
import { fetchWatchlist } from "../../lib/engagement-api";
import { ApiError } from "../../lib/api-client";

type ProfileStats = {
  ratingsCount: number;
  watchlistCount: number;
};

export default function ProfilePage() {
  const [authPresent, setAuthPresent] = useState(false);
  const [displayName, setDisplayName] = useState<string | null>(null);
  const [email, setEmail] = useState<string | null>(null);
  const [stats, setStats] = useState<ProfileStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const stored = loadAuth();

    if (!stored || !stored.token) {
      setAuthPresent(false);
      setDisplayName(null);
      setEmail(null);
      setLoading(false);
      return;
    }

    const token = stored.token;



    setAuthPresent(true);
    setDisplayName(stored.displayName ?? null);
    setEmail(stored.email ?? null);

    let cancelled = false;

    async function loadStats() {
      setLoading(true);
      setErrorMessage(null);

      try {
        const [ratings, watchlist] = await Promise.all([
          fetchMyRatings(token),
          fetchWatchlist(token),
        ]);

        if (cancelled) return;

        setStats({
          ratingsCount: ratings.length,
          watchlistCount: watchlist.length,
        });
      } catch (error) {
        if (cancelled) return;

        if (error instanceof ApiError && error.status === 401) {
          // Token expired/invalid – log the user out locally.
          clearAuth();
          setAuthPresent(false);
          setDisplayName(null);
          setEmail(null);
          setStats(null);
          setErrorMessage(
            "Your session has expired. Please sign in again to view your profile."
          );
        } else if (error instanceof ApiError) {
          const detail =
            error.problem?.detail ||
            error.problem?.title ||
            "Failed to load your profile data.";
          setErrorMessage(detail);
        } else {
          setErrorMessage("Failed to load your profile data.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadStats();

    return () => {
      cancelled = true;
    };
  }, []);

  // If the user isn't signed in at all, show a simple CTA.
  if (!authPresent) {
    return (
      <section className="flex w-full flex-col gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Your profile</h1>
          <p className="text-sm text-slate-300">
            Sign in to view your MicroFlix profile and activity.
          </p>
        </div>

        {errorMessage && (
          <div className="rounded-md border border-red-500 bg-red-950/40 px-3 py-2 text-xs text-red-200">
            {errorMessage}
          </div>
        )}

        <div className="mt-2 rounded-md border border-slate-800 bg-slate-900/60 p-4 text-sm">
          <p className="text-slate-200">
            You&apos;re not currently signed in.
          </p>
          <div className="mt-3 flex gap-3">
            <Link
              href="/login"
              className="rounded-md bg-sky-500 px-3 py-1 text-xs font-medium text-slate-950 hover:bg-sky-400"
            >
              Sign in
            </Link>
            <Link
              href="/login?mode=register"
              className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-sky-400 hover:text-sky-200"
            >
              Create an account
            </Link>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="flex w-full flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Your profile</h1>
        <p className="text-sm text-slate-300">
          Manage your MicroFlix account and see a quick summary of your
          activity.
        </p>
      </div>

      {errorMessage && (
        <div className="rounded-md border border-red-500 bg-red-950/40 px-3 py-2 text-xs text-red-200">
          {errorMessage}
        </div>
      )}

      {/* Account info card */}
      <div className="rounded-md border border-slate-800 bg-slate-900/60 p-4 text-sm">
        <h2 className="text-base font-semibold text-slate-100">Account</h2>
        <p className="mt-1 text-xs text-slate-400">
          Basic details for your MicroFlix account.
        </p>

        <dl className="mt-3 grid gap-3 sm:grid-cols-2">
          <div>
            <dt className="text-xs text-slate-400">Display name</dt>
            <dd className="text-sm font-medium text-slate-100">
              {displayName ?? "—"}
            </dd>
          </div>
          <div>
            <dt className="text-xs text-slate-400">Email</dt>
            <dd className="text-sm font-medium text-slate-100">
              {email ?? "—"}
            </dd>
          </div>
        </dl>

        <div className="mt-4 flex flex-wrap gap-2 text-xs">
          {/* These are placeholders for future settings pages */}
          <button
            type="button"
            className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-sky-400 hover:text-sky-200"
          >
            Change password (coming soon)
          </button>
          <button
            type="button"
            className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-sky-400 hover:text-sky-200"
          >
            Manage email &amp; security (coming soon)
          </button>
        </div>
      </div>

      {/* Activity summary card */}
      <div className="rounded-md border border-slate-800 bg-slate-900/60 p-4 text-sm">
        <h2 className="text-base font-semibold text-slate-100">
          Your activity
        </h2>
        <p className="mt-1 text-xs text-slate-400">
          High-level stats based on your ratings and watchlist.
        </p>

        {loading && !stats ? (
          <p className="mt-3 text-xs text-slate-400">Loading your stats…</p>
        ) : (
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            <div className="rounded-md border border-slate-700 bg-slate-900/80 p-3">
              <p className="text-xs text-slate-400">Ratings</p>
              <p className="mt-1 text-2xl font-semibold text-slate-100">
                {stats?.ratingsCount ?? 0}
              </p>
              <p className="mt-1 text-[11px] text-slate-400">
                Total movies you&apos;ve rated.
              </p>
              <Link
                href="/movies"
                className="mt-2 inline-block text-[11px] font-medium text-sky-300 hover:text-sky-200"
              >
                Browse movies →
              </Link>
            </div>

            <div className="rounded-md border border-slate-700 bg-slate-900/80 p-3">
              <p className="text-xs text-slate-400">Watchlist</p>
              <p className="mt-1 text-2xl font-semibold text-slate-100">
                {stats?.watchlistCount ?? 0}
              </p>
              <p className="mt-1 text-[11px] text-slate-400">
                Movies currently on your watchlist.
              </p>
              <Link
                href="/watchlist"
                className="mt-2 inline-block text-[11px] font-medium text-sky-300 hover:text-sky-200"
              >
                View watchlist →
              </Link>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
