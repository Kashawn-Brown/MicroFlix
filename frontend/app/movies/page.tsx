// Client-side Movies page: fetches a page of movies, handles loading/errors, and shows a simple movie grid

"use client"; // component runs in the browser

import { useEffect, useState } from "react";
import Image from "next/image";
import { fetchMoviesPage, type Movie } from "../../lib/movie-api";
import { ApiError } from "../../lib/api-client";

export default function MoviesPage() {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // fetch movies when the page loads
  useEffect(() => {

    // to avoid calling setState if the component unmounts before the request finishes
    let cancelled = false;

    async function load() {

      // Start fresh
      setLoading(true);
      setErrorMessage(null);

      try {
        // Call backend and try to get the first page with 12 movies
        const page = await fetchMoviesPage(0, 12);

        if (!cancelled) {
          // If successful, content will be the list of movies, so set them
          setMovies(page.content);
        }
      } catch (error) {
        if (!cancelled) {
          if (error instanceof ApiError) {

            const detail =
              error.problem?.detail || error.problem?.title || "Failed to load movies.";
            setErrorMessage(detail);

          } else {
            setErrorMessage("Failed to load movies.");
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();

    // cleanup in case component unmounts during request
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="flex w-full flex-col gap-4">
      <header className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Movies</h1>
        <p className="text-sm text-slate-300">
          Browse the MicroFlix catalog. We&apos;ll add search, filters, and
          pagination next.
        </p>
      </header>

      {loading && (
        <div className="text-sm text-slate-400">Loading movies...</div>
      )}

      {errorMessage && !loading && (
        <div className="rounded-md border border-red-500 bg-red-950/40 px-3 py-2 text-xs text-red-200">
          {errorMessage}
        </div>
      )}

      {!loading && !errorMessage && movies.length === 0 && (
        <div className="rounded-md border border-dashed border-slate-700 p-4 text-sm text-slate-400">
          No movies found yet. Once the movie-service is seeded, titles will
          show up here.
        </div>
      )}
      
      {!loading && !errorMessage && movies.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
          {movies.map((movie) => (
            <article
              key={movie.id}
              className="flex flex-col overflow-hidden rounded-lg border border-slate-800 bg-slate-900/40"
            >
              {/* Poster for movies */}
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
            </article>
          ))}
        </div>
      )}
    </section>
  );
}


// MOVIE LIST PAGE //
