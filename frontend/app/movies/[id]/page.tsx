// Client-side Movie Details page: loads movie + rating summary by id and shows a detailed view with error handling

import Image from "next/image";
import Link from "next/link";
import { fetchMovieById, type Movie } from "../../../lib/movie-api";
import {fetchRatingSummary, type RatingSummary,} from "../../../lib/rating-api";
import { ApiError } from "../../../lib/api-client";
import MovieActions from "../../../components/movie-actions";

// In Next 15, params is a Promise, so we type it that way.
type MovieDetailsPageProps = {
  params: Promise<{ id: string }>;
};

export default async function MovieDetailsPage({params,}: MovieDetailsPageProps) {
  // ❗ Important: await params before using .id
  const { id } = await params;
  const movieId = Number(id);

  // Guard against invalid ids
  if (Number.isNaN(movieId)) {
    return (
      <section className="flex w-full flex-col gap-4">
        <p className="text-sm text-red-300">Invalid movie id.</p>
        <Link
          href="/movies"
          className="text-sm text-sky-300 hover:text-sky-200"
        >
          ← Back to movies
        </Link>
      </section>
    );
  }

  // Variables for data and error handling
  let movie: Movie | null = null;
  let summary: RatingSummary | null = null;
  let errorMessage: string | null = null;

  try {
    
    // Ask for the movie and rating summary in parallel (wait for both to come back)
    const [movieData, summaryData] = await Promise.all([
      fetchMovieById(movieId),
      fetchRatingSummary(movieId),
    ]);

    // Set the movie data and summary
    movie = movieData;
    summary = summaryData;

  } catch (error) { 
    if (error instanceof ApiError) {
      errorMessage =
        error.problem?.detail ||
        error.problem?.title ||
        "Failed to load movie.";
    } else {
      errorMessage = "Failed to load movie.";
    }
  }

  // If no movie or error: show error page
  if (errorMessage || !movie) {
    return (
      <section className="flex w-full flex-col gap-4">
        <p className="text-sm text-red-300">
          {errorMessage ?? "Movie not found."}
        </p>
        <Link
          href="/movies"
          className="text-sm text-sky-300 hover:text-sky-200"
        >
          ← Back to movies
        </Link>
      </section>
    );
  }

  // Retrive average ratings, and make sure it is presentable
  const average =
    summary && summary.average !== null
      ? summary.average.toFixed(1)
      : null;


  return (
    <section className="flex w-full flex-col gap-6">
      <Link
        href="/movies"
        className="text-xs text-sky-300 hover:text-sky-200"
      >
        ← Back to movies
      </Link>

      <div className="flex flex-col gap-6 md:flex-row">
        <div className="w-full md:w-1/3">
          {movie.posterUrl ? (
            <div className="relative aspect-[2/3] overflow-hidden rounded-lg bg-slate-800">
              <Image
                src={movie.posterUrl}
                alt={movie.title}
                fill
                className="object-cover"
                sizes="(min-width: 1024px) 20rem, 50vw"
              />
            </div>
          ) : (
            <div className="flex aspect-[2/3] items-center justify-center rounded-lg bg-slate-800 text-xs text-slate-500">
              No poster
            </div>
          )}
        </div>

        <div className="flex flex-1 flex-col gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-semibold tracking-tight">
              {movie.title}
            </h1>
            {movie.releaseYear && (
              <p className="text-sm text-slate-400">{movie.releaseYear}</p>
            )}
            {movie.genres?.length > 0 && (
              <p className="text-sm text-slate-300">
                {movie.genres.join(" • ")}
              </p>
            )}
          </div>

          {movie.overview && (
            <p className="text-sm text-slate-200 leading-relaxed">
              {movie.overview}
            </p>
          )}

          <div className="mt-2 rounded-md border border-slate-800 bg-slate-900/60 p-3 text-sm">
            <h2 className="text-sm font-semibold text-slate-100">
              Ratings
            </h2>
            {summary && summary.count > 0 && average ? (
              <p className="mt-1 text-slate-200">
                Average rating:{" "}
                <span className="font-semibold">{average}</span>/10 from{" "}
                <span className="font-semibold">{summary.count}</span>{" "}
                rating{summary.count === 1 ? "" : "s"}.
              </p>
            ) : (
              <p className="mt-1 text-slate-400 text-sm">
                No ratings yet. We&apos;ll add rating controls here soon.
              </p>
            )}
          </div>

          {/* Client-side controls for your rating + watchlist */}
          <MovieActions movieId={movieId} />
        </div>


      </div>
    </section>
  );
}
