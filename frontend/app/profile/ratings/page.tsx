"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";

import { loadAuth } from "../../../lib/auth-storage";
import {
  fetchMyRatings,
  deleteMyRating,
  type RatingResponse,
} from "../../../lib/rating-api";
import { fetchMovieById, type Movie } from "../../../lib/movie-api";
import { ApiError } from "../../../lib/api-client";

type RatingWithMovie = {
  rating: RatingResponse;
  movie: Movie;
};

function formatRating(rate: number): string {
  // If it's a whole number like 9 or 10, show "9" / "10"
  if (Number.isInteger(rate)) {
    return rate.toString();
  }
  // Otherwise keep one decimal place, e.g. 8.5
  return rate.toFixed(1);
}

/**
 * My Ratings page
 * - Requires a logged-in user (reads JWT from localStorage).
 * - Loads all of the current user's ratings.
 * - For each rating, fetches the movie details so we can render nice cards.
 */
export default function MyRatingsPage() {
  const [authPresent, setAuthPresent] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [displayName, setDisplayName] = useState<string | null>(null);

  const [items, setItems] = useState<RatingWithMovie[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [removingId, setRemovingId] = useState<number | null>(null);

  // Load auth and then the user's ratings.
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

    let cancelled = false;

    async function loadRatings() {
      setLoading(true);
      setErrorMessage(null);

      try {
        // 1) Fetch the raw ratings for this user.
        const ratings = await fetchMyRatings(token);

        if (cancelled) return;

        if (ratings.length === 0) {
          setItems([]);
          return;
        }

        // 2) Fetch movie details for each unique movieId.
        const movieIds = Array.from(
          new Set(ratings.map((r) => r.movieId))
        );
        const movies = await Promise.all(
          movieIds.map((id) => fetchMovieById(id))
        );

        if (cancelled) return;

        const moviesById = new Map<number, Movie>();
        for (const movie of movies) {
          moviesById.set(movie.id, movie);
        }

        // 3) Join ratings with movies and drop any that we
        //    couldn't resolve a movie for (should be rare).
        const joined: RatingWithMovie[] = ratings
          .map((rating) => {
            const movie = moviesById.get(rating.movieId);
            if (!movie) return null;
            return { rating, movie };
          })
          .filter((x): x is RatingWithMovie => x !== null);

        setItems(joined);
      } catch (error) {
        if (cancelled) return;

        if (error instanceof ApiError) {
          const detail =
            error.problem?.detail ||
            error.problem?.title ||
            "Failed to load your ratings.";
          setErrorMessage(detail);
        } else {
          setErrorMessage("Failed to load your ratings.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadRatings();

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleRemoveRating(movieId: number) {
    if (!token) return;
    setRemovingId(movieId);
    setErrorMessage(null);

    try {
      await deleteMyRating(movieId, token);
      // Optimistically update UI.
      setItems((prev) => prev.filter((item) => item.movie.id !== movieId));
    } catch (error) {
      if (error instanceof ApiError) {
        const detail =
          error.problem?.detail ||
          error.problem?.title ||
          "Failed to remove rating.";
        setErrorMessage(detail);
      } else {
        setErrorMessage("Failed to remove rating.");
      }
    } finally {
      setRemovingId(null);
    }
  }

  // If not logged in: prompt to sign in.
  if (!authPresent) {
    return (
      <section className="flex w-full flex-col gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            My ratings
          </h1>
          <p className="text-sm text-slate-300">
            Sign in to see the movies you&apos;ve rated.
          </p>
        </div>

        <div className="mt-3 rounded-md border border-slate-800 bg-slate-900/60 p-3 text-sm">
          <p className="text-slate-200">
            You&apos;re not signed in. Log in to view and manage your ratings.
          </p>
          <Link
            href="/login"
            className="mt-2 inline-block text-xs text-sky-300 hover:text-sky-200"
          >
            Go to login →
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="flex w-full flex-col gap-4">
        <Link
        href="/profile"
        className="text-xs text-sky-300 hover:text-sky-200"
      >
        ← Back to profile
      </Link>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">
          My ratings
        </h1>
        <p className="text-sm text-slate-300">
          Showing movies rated by{" "}
          <span className="font-medium">
            {displayName ?? "you"}
          </span>
          .
        </p>
      </div>

      {errorMessage && (
        <div className="rounded-md border border-red-500 bg-red-950/40 px-3 py-2 text-xs text-red-200">
          {errorMessage}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-slate-300">Loading your ratings…</p>
      ) : items.length === 0 ? (
        <p className="text-sm text-slate-300">
          You haven&apos;t rated any movies yet. Browse the catalog and
          leave a few ratings to see them here.
        </p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {items.map(({ movie, rating }) => (
            <div
              key={rating.id}
              className="flex flex-col overflow-hidden rounded-lg border border-slate-800 bg-slate-900/40"
            >
              <Link href={`/movies/${movie.id}`}>
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
              </Link>

              <div className="flex flex-1 flex-col gap-2 p-3">
                <div>
                  <Link
                    href={`/movies/${movie.id}`}
                    className="text-sm font-semibold hover:text-sky-300"
                  >
                    {movie.title}
                  </Link>
                  {movie.releaseYear && (
                    <p className="text-xs text-slate-400">
                      {movie.releaseYear}
                    </p>
                  )}
                </div>

                <p className="text-xs text-slate-300">
                  Your rating:{" "}
                  <span className="font-semibold">
                    {formatRating(rating.rate)}
                  </span>
                  /10
                </p>

                <div className="mt-1 flex gap-2">
                  <Link
                    href={`/movies/${movie.id}`}
                    className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-sky-400 hover:text-sky-200"
                  >
                    View details
                  </Link>
                  <button
                    type="button"
                    onClick={() => handleRemoveRating(movie.id)}
                    disabled={removingId === movie.id}
                    className="rounded-md border border-slate-700 px-3 py-1 text-xs font-medium text-slate-300 hover:border-red-500 hover:text-red-200 disabled:cursor-not-allowed disabled:opacity-70"
                  >
                    {removingId === movie.id
                      ? "Removing…"
                      : "Remove rating"}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
