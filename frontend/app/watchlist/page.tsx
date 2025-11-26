// Client-side Watchlist page: loads user's watchlist + movie details and lets them remove items

"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { loadAuth } from "../../lib/auth-storage";
import {
  fetchWatchlist,
  removeFromWatchlist,
  type EngagementItem,
} from "../../lib/engagement-api";
import { fetchMovieById, type Movie } from "../../lib/movie-api";
import { ApiError } from "../../lib/api-client";

// Combined type to store 1 clean list of Watchlist entries
type WatchlistEntry = {
  movie: Movie;
  addedAt: string;
};

export default function WatchlistPage() {
  const [authPresent, setAuthPresent] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [displayName, setDisplayName] = useState<string | null>(null);

  const [entries, setEntries] = useState<WatchlistEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Load auth and then the user's watchlist with movie details.
  useEffect(() => {
    const stored = loadAuth();
    if (!stored || !stored.token) {
      setAuthPresent(false);
      setToken(null);
      setDisplayName(null);
      setLoading(false);
      return;
    }

    const token = stored.token;
    setAuthPresent(true);
    setToken(token);
    setDisplayName(stored.displayName ?? null);

    // cancellation flag in case the component unmounts before async calls finish
    let cancelled = false;

    async function loadWatchlist() {
      setLoading(true);
      setErrorMessage(null);

      try {
        // Fetch users watchlist
        const engagements: EngagementItem[] = await fetchWatchlist(token);

        if (cancelled) return;

        if (engagements.length === 0) {
          setEntries([]);
          return;
        }

        // Fetch movie details for each watchlist item.
        const movies: Movie[] = await Promise.all(
          engagements.map((eng) => fetchMovieById(eng.movieId))
        );

        if (cancelled) return;

        // Combine each engagement with its corresponding movie by index
        const combined: WatchlistEntry[] = engagements.map((eng, index) => ({
          movie: movies[index],
          addedAt: eng.addedAt,
        }));

        setEntries(combined);
      } catch (error) {
        if (cancelled) return;

        if (error instanceof ApiError && error.status === 401) {
          setAuthPresent(false);
          setToken(null);
          setDisplayName(null);
        } else if (error instanceof ApiError) {
          const detail =
            error.problem?.detail ||
            error.problem?.title ||
            "Failed to load watchlist.";
          setErrorMessage(detail);
        } else {
          setErrorMessage("Failed to load watchlist.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadWatchlist();

    return () => {
      cancelled = true;
    };
  }, []);

  // Handler to remove a movie from watchlist
  async function handleRemove(movieId: number) {
    if (!token) return;
    setRemovingId(movieId);
    setErrorMessage(null);

    try {
      await removeFromWatchlist(movieId, token);
      setEntries((prev) => prev.filter((entry) => entry.movie.id !== movieId));
    } catch (error) {
      if (error instanceof ApiError) {
        const detail =
          error.problem?.detail ||
          error.problem?.title ||
          "Failed to update watchlist.";
        setErrorMessage(detail);
      } else {
        setErrorMessage("Failed to update watchlist.");
      }
    } finally {
      setRemovingId(null);
    }
  }

  // Page to show if user not authenticated
  if (!authPresent && !loading) {
    return (
      <section className="flex w-full flex-col gap-4">
        <h1 className="text-2xl font-semibold tracking-tight">Watchlist</h1>
        <p className="text-sm text-slate-300">
          Sign in to view and manage your watchlist.
        </p>
        <Link
          href="/login"
          className="mt-1 inline-block text-sm text-sky-300 hover:text-sky-200"
        >
          Go to login →
        </Link>
      </section>
    );
  }

  return (
    <section className="flex w-full flex-col gap-4">
      <h1 className="text-2xl font-semibold tracking-tight">Watchlist</h1>

      {displayName && (
        <p className="text-xs text-slate-400">
          Showing watchlist for{" "}
          <span className="font-medium text-slate-100">{displayName}</span>
        </p>
      )}

      {errorMessage && (
        <div className="rounded-md border border-red-500 bg-red-950/40 px-3 py-2 text-xs text-red-200">
          {errorMessage}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-slate-400">Loading your watchlist…</p>
      ) : entries.length === 0 ? (
        <p className="text-sm text-slate-300">
          Your watchlist is empty. Browse{" "}
          <Link href="/movies" className="text-sky-300 hover:text-sky-200">
            Movies
          </Link>{" "}
          and add some titles to get started.
        </p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {entries.map(({ movie, addedAt }) => (
            <article
              key={movie.id}
              className="flex flex-col overflow-hidden rounded-lg border border-slate-800 bg-slate-900/40"
            >
              {movie.posterUrl ? (
                <div className="relative aspect-[2/3] bg-slate-800">
                  <Image
                    src={movie.posterUrl}
                    alt={movie.title}
                    fill
                    className="object-cover"
                    sizes="(min-width: 1024px) 16rem, (min-width: 768px) 33vw, 50vw"
                  />
                </div>
              ) : (
                <div className="flex aspect-[2/3] items-center justify-center bg-slate-800 text-xs text-slate-500">
                  No poster
                </div>
              )}

              <div className="flex flex-1 flex-col gap-2 p-3">
                <div className="space-y-1">
                  <h2 className="line-clamp-2 text-sm font-semibold">
                    {movie.title}
                  </h2>
                  {movie.releaseYear && (
                    <p className="text-xs text-slate-400">
                      {movie.releaseYear}
                    </p>
                  )}
                  {movie.genres?.length > 0 && (
                    <p className="line-clamp-1 text-xs text-slate-400">
                      {movie.genres.join(" • ")}
                    </p>
                  )}
                </div>

                <p className="mt-1 text-[11px] text-slate-500">
                  Added at:{" "}
                  <span className="font-medium">
                    {new Date(addedAt).toLocaleString()}
                  </span>
                </p>

                <div className="mt-2 flex items-center gap-2">
                  <Link
                    href={`/movies/${movie.id}`}
                    className="rounded-md bg-sky-500 px-3 py-1 text-xs font-medium text-slate-950 hover:bg-sky-400"
                  >
                    View details
                  </Link>
                  <button
                    type="button"
                    onClick={() => handleRemove(movie.id)}
                    disabled={removingId === movie.id}
                    className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-red-500 hover:text-red-200 disabled:cursor-not-allowed disabled:opacity-70"
                  >
                    {removingId === movie.id ? "Removing…" : "Remove"}
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

// USERS WATCHLIST PAGE //
