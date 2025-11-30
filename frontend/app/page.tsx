
import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { fetchMoviesPage, type Movie } from "../lib/movie-api";
import { ApiError } from "../lib/api-client";

// Do not pre-render page at build time. Run HomePage() on the server every time someone requests /
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "MicroFlix – Home",
};

export default async function HomePage() {
  let featured: Movie | null = null;
  let others: Movie[] = [];
  let loadError: string | null = null;

  // Try to load a small set of recently added movies for the homepage.
  try {
    const page = await fetchMoviesPage({
      page: 0,
      size: 6,
      sort: "created_desc", // newest added first
    });

    if (page.content.length > 0) {
      featured = page.content[0];
      others = page.content.slice(1, 5);
    }
  } catch (error) {
    if (error instanceof ApiError) {
      loadError =
        error.problem?.detail ||
        error.problem?.title ||
        "Failed to load featured movies.";
    } else {
      loadError = "Failed to load featured movies.";
    }
  }

  return (
    <section className="flex w-full flex-col gap-6">
      {/* Intro + primary actions */}
      <div className="space-y-3">
        <h1 className="text-3xl font-semibold tracking-tight">
          Welcome to MicroFlix
        </h1>
        <p className="text-sm text-slate-300">
          Browse movies, rate what you watch, and build your own watchlist. <br/>
        </p>
        <div className="flex flex-wrap gap-4">
          <Link
            href="/movies"
            className="text-sm text-sky-300 hover:text-sky-200"
          >
            Browse all movies →
          </Link>
        </div>
      </div>

      {/* Featured content */}
      {loadError ? (
        <p className="text-sm text-slate-300">{loadError}</p>
      ) : featured ? (
        <>
          {/* Hero featured movie */}
          <section className="flex flex-col gap-4 rounded-lg border border-slate-800 bg-slate-900/60 p-4 md:flex-row">
            <div className="relative aspect-[16/9] w-full overflow-hidden rounded-md bg-slate-800 md:w-1/2 lg:w-2/5">
              {featured.backdropUrl || featured.posterUrl ? (
                <Image
                  src={featured.backdropUrl || featured.posterUrl!}
                  alt={featured.title}
                  fill
                  className="object-cover"
                  sizes="(min-width: 1024px) 24rem, (min-width: 768px) 50vw, 100vw"
                />
              ) : (
                <div className="flex h-full items-center justify-center text-xs text-slate-500">
                  No image available
                </div>
              )}
            </div>

            <div className="flex flex-1 flex-col gap-3">
              <p className="text-xs font-medium uppercase tracking-wide text-sky-300">
                Featured movie
              </p>
              <h2 className="text-xl font-semibold">{featured.title}</h2>
              <p className="text-xs text-slate-400">
                {featured.releaseYear && (
                  <span className="mr-2">{featured.releaseYear}</span>
                )}
                {featured.genres && featured.genres.length > 0 && (
                  <span>{featured.genres.join(" • ")}</span>
                )}
              </p>
              <p className="line-clamp-4 text-sm text-slate-200">
                {featured.overview && featured.overview.trim().length > 0
                  ? featured.overview
                  : "No overview available for this title yet."}
              </p>
              <div className="mt-2 flex flex-wrap gap-2">
                <Link
                  href={`/movies/${featured.id}`}
                  className="rounded-md bg-sky-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400"
                >
                  View details
                </Link>
                <Link
                  href="/movies"
                  className="text-sm text-sky-300 hover:text-sky-200"
                >
                  Explore the catalog →
                </Link>
              </div>
            </div>
          </section>

          {/* Recently added row */}
          {others.length > 0 && (
            <section className="space-y-2">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-semibold text-slate-100">
                  Recently added
                </h2>
                <Link
                  href="/movies"
                  className="text-xs text-sky-300 hover:text-sky-200"
                >
                  View all →
                </Link>
              </div>
              <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
                {others.map((movie) => (
                  <Link
                    key={movie.id}
                    href={`/movies/${movie.id}`}
                    className="flex flex-col overflow-hidden rounded-lg border border-slate-800 bg-slate-900/40 hover:border-sky-500"
                  >
                    {movie.posterUrl ? (
                      <div className="relative aspect-[2/3] bg-slate-800">
                        <Image
                          src={movie.posterUrl}
                          alt={movie.title}
                          fill
                          className="object-cover"
                          sizes="(min-width: 1024px) 12rem, (min-width: 768px) 33vw, 50vw"
                        />
                      </div>
                    ) : (
                      <div className="flex aspect-[2/3] items-center justify-center bg-slate-800 text-xs text-slate-500">
                        No poster
                      </div>
                    )}
                    <div className="flex flex-1 flex-col gap-1 p-3">
                      <h3 className="line-clamp-2 text-sm font-semibold">
                        {movie.title}
                      </h3>
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
                  </Link>
                ))}
              </div>
            </section>
          )}
        </>
      ) : null}
    </section>
  );
}


// HOME PAGE //


