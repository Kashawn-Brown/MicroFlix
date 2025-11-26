// Client-side movie actions: load user rating + watchlist for this movie and provide controls to change them

"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { loadAuth } from "../lib/auth-storage";
import {
    deleteMyRating,
    fetchMyRatingForMovie,
    upsertMyRating,
    type RatingResponse,
} from "../lib/rating-api";
import {
    addToWatchlist,
    fetchInWatchlist,
    removeFromWatchlist,
} from "../lib/engagement-api";
import { ApiError } from "../lib/api-client";

type MovieActionsProps = {
  movieId: number;
};

export default function MovieActions({ movieId }: MovieActionsProps) {
    // Components to keep track of
    const [authPresent, setAuthPresent] = useState(false);
    const [token, setToken] = useState<string | null>(null);
    const [displayName, setDisplayName] = useState<string | null>(null);

    const [rating, setRating] = useState<RatingResponse | null>(null);
    const [ratingInput, setRatingInput] = useState<string>("");
    const [inWatchlist, setInWatchlist] = useState(false);

    const [loading, setLoading] = useState(true);
    const [savingRating, setSavingRating] = useState(false);
    const [togglingWatchlist, setTogglingWatchlist] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // Load auth from localStorage and, if present, load rating + watchlist state.
    useEffect(() => {

        // Checking for authentication
        const stored = loadAuth();
        if (!stored) {
        setAuthPresent(false);
        setToken(null);
        setDisplayName(null);
        setLoading(false);
        return;
        }

        const token = stored.token;

        if (!token) {
        setAuthPresent(false);
        setToken(null);
        setDisplayName(null);
        setLoading(false);
        return;
        }

        // If authenticated and token is found, mark as logged in
        setAuthPresent(true);
        setToken(token);
        setDisplayName(stored.displayName ?? null);

        // set up a cancellation flag in case the component unmounts before async calls finish
        let cancelled = false;

        async function loadUserState() {
            setLoading(true);
            setErrorMessage(null);

            try {
                // Fetch the user's rating for this movie + whether it's in their watchlist. (in parrallel)
                const [myRating, inWatchlistFlag] = await Promise.all([
                fetchMyRatingForMovie(movieId, token),
                fetchInWatchlist(movieId, token),
                ]);

                if (cancelled) return;

                setRating(myRating);
                setRatingInput(myRating ? myRating.rate.toFixed(1) : "");
                setInWatchlist(inWatchlistFlag);
            } catch (error) {
                if (cancelled) return;

                if (error instanceof ApiError && error.status === 401) {
                    // Token expired/invalid – treat as logged out for now.
                    setAuthPresent(false);
                    setToken(null);
                    setDisplayName(null);
                } else if (error instanceof ApiError) {
                    const detail =
                        error.problem?.detail ||
                        error.problem?.title ||
                        "Failed to load your movie state.";
                    setErrorMessage(detail);
                } else {
                    setErrorMessage("Failed to load your movie state.");
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        // Start loading user-specific state w function defined above
        loadUserState();

        return () => {
            cancelled = true;
        };
    }, [movieId]);

    // Handler to save rating (runs when user hits 'save rating')
    async function handleSaveRating(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();  // prevent from reloading page

        if (!token) return;

        const value = parseFloat(ratingInput);

        if (Number.isNaN(value)) {
            setErrorMessage("Please enter a number between 1.0 and 10.0.");
            return;
        }
        if (value < 1 || value > 10) {
            setErrorMessage("Rating must be between 1.0 and 10.0.");
            return;
        }

        setSavingRating(true);
        setErrorMessage(null);

        try {
            const updated = await upsertMyRating(movieId, value, token);
            setRating(updated);
            setRatingInput(updated.rate.toFixed(1));
        } catch (error) {
            if (error instanceof ApiError) {
                const detail =
                error.problem?.detail ||
                error.problem?.title ||
                "Failed to save rating.";
                setErrorMessage(detail);
            } else {
                setErrorMessage("Failed to save rating.");
            }
        } finally {
            setSavingRating(false);
        }
    }

    // Handler to delete rating (runs when user hits 'remove')
    async function handleDeleteRating() {
        if (!token) return;
        
        setSavingRating(true);
        setErrorMessage(null);

        try {
            await deleteMyRating(movieId, token);
            setRating(null);
            setRatingInput("");
        } catch (error) {
            if (error instanceof ApiError) {
                const detail =
                error.problem?.detail ||
                error.problem?.title ||
                "Failed to delete rating.";
                setErrorMessage(detail);
            } else {
                setErrorMessage("Failed to delete rating.");
            }
        } finally {
            setSavingRating(false);
        }
    }

    // Handler to toggle adding/removinf movie from users watchlist
    async function handleToggleWatchlist() {
        if (!token) return;
        
        setTogglingWatchlist(true);
        setErrorMessage(null);

        try {
            if (inWatchlist) {
                await removeFromWatchlist(movieId, token);
                setInWatchlist(false);
            } else {
                await addToWatchlist(movieId, token);
                setInWatchlist(true);
            }
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
            setTogglingWatchlist(false);
        }
    }

    // How page is rendered when user is not logged in
    if (!authPresent) {
        return (
        <div className="mt-3 rounded-md border border-slate-800 bg-slate-900/60 p-3 text-sm">
            <p className="text-slate-200">
            Sign in to rate this movie and manage your watchlist.
            </p>
            <Link
            href="/login"
            className="mt-2 inline-block text-xs text-sky-300 hover:text-sky-200"
            >
            Go to login →
            </Link>
        </div>
        );
    }

    // How page is rendered when user is logged in
    return (
        <div className="mt-3 flex flex-col gap-4 rounded-md border border-slate-800 bg-slate-900/60 p-3 text-sm">
        <div className="flex flex-col gap-1">
            <p className="text-xs text-slate-400">
            Signed in as{" "}
            <span className="font-medium text-slate-100">
                {displayName ?? "you"}
            </span>
            </p>
        </div>

        {errorMessage && (
            <div className="rounded-md border border-red-500 bg-red-950/40 px-3 py-2 text-xs text-red-200">
            {errorMessage}
            </div>
        )}

        {loading ? (
            <p className="text-xs text-slate-400">Loading your rating…</p>
        ) : (
            <div className="flex flex-col gap-4 md:flex-row">
            {/* Rating section */}
            <form
                onSubmit={handleSaveRating}
                className="flex flex-1 flex-col gap-2"
            >
                <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-100">
                    Your rating
                </h3>
                {rating && (
                    <p className="text-xs text-slate-400">
                    Current:{" "}
                    <span className="font-semibold">
                        {rating.rate.toFixed(1)}
                    </span>
                    /10
                    </p>
                )}
                </div>

                <div className="flex items-center gap-2">
                <input
                    type="number"
                    min={1}
                    max={10}
                    step={0.1}
                    value={ratingInput}
                    onChange={(e) => setRatingInput(e.target.value)}
                    className="w-24 rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
                <button
                    type="submit"
                    disabled={savingRating}
                    className="rounded-md bg-sky-500 px-3 py-1 text-xs font-medium text-slate-950 hover:bg-sky-400 disabled:cursor-not-allowed disabled:opacity-70"
                >
                    {savingRating ? "Saving…" : "Save rating"}
                </button>
                {rating && (
                    <button
                    type="button"
                    onClick={handleDeleteRating}
                    disabled={savingRating}
                    className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-red-500 hover:text-red-200 disabled:cursor-not-allowed disabled:opacity-70"
                    >
                    Remove
                    </button>
                )}
                </div>
                <p className="text-[11px] text-slate-400">
                Enter a value between 1.0 and 10.0. We store this as an integer
                under the hood to avoid rounding issues.
                </p>
            </form>

            {/* Watchlist section */}
            <div className="flex flex-1 flex-col gap-2">
                <h3 className="text-sm font-semibold text-slate-100">
                Watchlist
                </h3>
                <p className="text-xs text-slate-400">
                {inWatchlist
                    ? "This movie is on your watchlist."
                    : "Add this movie to your watchlist to find it later."}
                </p>
                <button
                type="button"
                onClick={handleToggleWatchlist}
                disabled={togglingWatchlist}
                className="mt-1 w-fit rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-sky-400 hover:text-sky-200 disabled:cursor-not-allowed disabled:opacity-70"
                >
                {togglingWatchlist
                    ? "Updating…"
                    : inWatchlist
                    ? "Remove from watchlist"
                    : "Add to watchlist"}
                </button>
            </div>
            </div>
        )}
        </div>
    );
}
