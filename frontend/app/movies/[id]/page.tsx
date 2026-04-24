// Server-rendered Movie Details page: loads movie + rating summary via the gateway's
// aggregation endpoint in one hit, then hands the authed per-user slice off to MovieActions (CSR).

import type { Metadata, ResolvingMetadata } from "next";
import Image from "next/image";
import Link from "next/link";
import type { Movie } from "../../../lib/movie-api";
import type { RatingSummary } from "../../../lib/rating-api";
import { fetchCatalogMovieDetails } from "../../../lib/catalog-api";
import { ApiError } from "../../../lib/api-client";
import MovieActions from "../../../components/movie-actions";

export async function generateMetadata(
  { params }: MovieDetailsPageProps,
  _parent: ResolvingMetadata
): Promise<Metadata> {
  const { id } = await params;
  const movieId = Number(id);

  if (Number.isNaN(movieId)) {
    return {
      title: "Movie not found – MicroFlix",
    };
  }

  try {
    // Same endpoint the page body uses. Next.js dedups per-request fetches, so metadata
    // + page rendering share a single network round-trip to the gateway.
    const { movie } = await fetchCatalogMovieDetails(movieId);

    if (!movie) {
      return {
        title: "Movie not found – MicroFlix",
      };
    }

    const yearPart = movie.releaseYear ? ` (${movie.releaseYear})` : "";
    const title = `${movie.title}${yearPart} – MicroFlix`;

    const description =
      movie.overview?.slice(0, 180) ||
      `Details and ratings for ${movie.title} on MicroFlix.`;

    return {
      title,
      description,
      openGraph: {
        title,
        description,
        images: movie.posterUrl
          ? [
              {
                url: movie.posterUrl,
                alt: movie.title,
              },
            ]
          : undefined,
      },
      twitter: {
        card: "summary_large_image",
        title,
        description,
      },
    };
  } catch {
    // If the fetch fails, don’t break the page – just fall back.
    return {
      title: "Movie – MicroFlix",
    };
  }
}

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
  let ratingSummary: RatingSummary | null = null;
  let errorMessage: string | null = null;

  try {

    // One aggregate call instead of two parallel downstream fetches. The me section
    // comes back as the anonymous view (no Authorization header from SSR); MovieActions
    // fills it in on the client.
    const details = await fetchCatalogMovieDetails(movieId);

    movie = details.movie;
    ratingSummary = details.ratingSummary;

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

          <section className="mt-4 rounded-md border border-slate-800 bg-slate-900/60 p-4">
            <h2 className="text-sm font-semibold text-slate-100">Ratings</h2>

            {ratingSummary && ratingSummary.count > 0 && ratingSummary.average !== null ? (
              <div className="mt-3 flex items-baseline gap-3">
                <div className="flex items-baseline gap-2">
                  <span className="text-2xl font-semibold text-slate-100">
                    {ratingSummary.average.toFixed(1)}
                  </span>
                  <span className="text-sm text-slate-400">/10</span>
                </div>
                <p className="text-xs text-slate-400">
                  from{" "}
                  <span className="font-medium">
                    {ratingSummary.count}
                  </span>{" "}
                  rating{ratingSummary.count === 1 ? "" : "s"}
                </p>
              </div>
            ) : (
              <p className="mt-3 text-sm text-slate-300">
                No ratings yet. Be the first to rate this movie.
              </p>
            )}
          </section>
          

          {/* Client-side controls for your rating + watchlist */}
          <MovieActions movieId={movieId} />
        </div>


      </div>
    </section>
  );
}
