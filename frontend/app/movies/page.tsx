// Client-side Movies page: fetches a page of movies, handles loading/errors, and shows a simple movie grid

import Image from "next/image";
import Link from "next/link";
import {
  fetchMoviesPage,
  type Movie,
  type Page as PageType,
} from "../../lib/movie-api";
import { ApiError } from "../../lib/api-client";

type MoviesPageProps = {
  searchParams?: Promise<{
    query?: string;
    genre?: string;
    year?: string;
    sort?: string;
    page?: string;
    size?: string;
  }>;
};

// helper for pagination links (builds URLSearchParams without page parameter)
function buildBaseParams(
  query?: string,
  genre?: string,
  year?: string,
  sort?: string,
  size?: number
): URLSearchParams {
  const params = new URLSearchParams();
  if (query) params.set("query", query);
  if (genre) params.set("genre", genre);
  if (year) params.set("year", year);
  if (sort) params.set("sort", sort);
  if (typeof size === "number") params.set("size", String(size));
  return params;
}

export default async function MoviesPage({ searchParams }: MoviesPageProps) {
  // In Next 15, searchParams is a Promise.
  const resolved = (await searchParams) ?? {};

  // destructure
  const {
    query = "",
    genre = "",
    year = "",
    sort = "",
    page = "0",
    size = "12",
  } = resolved;

  const pageIndex = Number(page) || 0;
  const pageSize = Number(size) || 12;
  const yearNumber = year ? Number(year) : undefined;

  let moviesPage: PageType<Movie> | null = null;
  let errorMessage: string | null = null;

  // Calling fetchMoviesPage with filters
  try {
    moviesPage = await fetchMoviesPage({
      page: pageIndex,
      size: pageSize,
      query: query || undefined,
      genre: genre || undefined,
      year: yearNumber,
      sort: sort || undefined,
    });
  } catch (error) {
    if (error instanceof ApiError) {
      errorMessage =
        error.problem?.detail ||
        error.problem?.title ||
        "Failed to load movies.";
    } else {
      errorMessage = "Failed to load movies.";
    }
  }

  const currentPage = moviesPage?.number ?? pageIndex;
  const totalPages = moviesPage?.totalPages ?? 0;

  const baseParams = buildBaseParams(
    query || undefined,
    genre || undefined,
    year || undefined,
    sort || undefined,
    pageSize
  );

  const hasPrev = currentPage > 0;
  const hasNext = totalPages > 0 && currentPage < totalPages - 1;

  const prevParams = new URLSearchParams(baseParams);
  prevParams.set("page", String(Math.max(currentPage - 1, 0)));

  const nextParams = new URLSearchParams(baseParams);
  nextParams.set("page", String(currentPage + 1));

  return (
    <section className="flex w-full flex-col gap-4">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Movies</h1>
          <p className="text-sm text-slate-300">
            Browse the MicroFlix catalog. Use search and filters to narrow down
            what you&apos;re looking for.
          </p>
        </div>

        {/* Filter form submits via GET so filters appear in the URL. */}
        <form
          method="GET"
          className="mt-2 flex flex-col gap-2 text-sm md:mt-0 md:flex-row md:items-end"
        >
          <div className="flex flex-col gap-1">
            <label
              htmlFor="query"
              className="text-xs font-medium text-slate-300"
            >
              Search
            </label>
            <input
              id="query"
              name="query"
              type="text"
              defaultValue={query}
              placeholder="Title contains…"
              className="w-48 rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label
              htmlFor="genre"
              className="text-xs font-medium text-slate-300"
            >
              Genre
            </label>
            <input
              id="genre"
              name="genre"
              type="text"
              defaultValue={genre}
              placeholder="Action"
              className="w-32 rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label
              htmlFor="year"
              className="text-xs font-medium text-slate-300"
            >
              Year
            </label>
            <input
              id="year"
              name="year"
              type="number"
              min={1900}
              max={2100}
              defaultValue={year}
              className="w-24 rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label
              htmlFor="sort"
              className="text-xs font-medium text-slate-300"
            >
              Sort
            </label>
            <select
              id="sort"
              name="sort"
              defaultValue={sort || "created_desc"}
              className="w-40 rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="created_desc">Newest added</option>
              <option value="created_asc">Oldest added</option>
              <option value="title_asc">Title A–Z</option>
              <option value="title_desc">Title Z–A</option>
              <option value="year_desc">Year desc</option>
              <option value="year_asc">Year asc</option>
            </select>
          </div>

          {/* Keep page size, but reset page to 0 when applying filters. */}
          <input type="hidden" name="size" value={pageSize} />
          <input type="hidden" name="page" value="0" />

          <button
            type="submit"
            className="rounded-md bg-sky-500 px-3 py-1 text-xs font-medium text-slate-950 hover:bg-sky-400 md:self-end"
          >
            Apply
          </button>
        </form>
      </div>

      {errorMessage && (
        <div className="rounded-md border border-red-500 bg-red-950/40 px-3 py-2 text-xs text-red-200">
          {errorMessage}
        </div>
      )}

      {!moviesPage || moviesPage.content.length === 0 ? (
        <p className="text-sm text-slate-300">
          No movies found. Try adjusting your search or filters.
        </p>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {moviesPage.content.map((movie) => (
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
                      sizes="(min-width: 1024px) 16rem, (min-width: 768px) 33vw, 50vw"
                    />
                  </div>
                ) : (
                  <div className="flex aspect-[2/3] items-center justify-center bg-slate-800 text-xs text-slate-500">
                    No poster
                  </div>
                )}

                <div className="flex flex-1 flex-col gap-1 p-3">
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
              </Link>
            ))}
          </div>

          {/* Pagination controls */}
          <div className="mt-4 flex items-center justify-between text-xs text-slate-300">
            <p>
              Page {currentPage + 1} of {totalPages || 1}
            </p>
            <div className="flex items-center gap-2">
              {hasPrev && (
                <Link
                  href={`/movies?${prevParams.toString()}`}
                  className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-sky-400 hover:text-sky-200"
                >
                  ← Previous
                </Link>
              )}
              {hasNext && (
                <Link
                  href={`/movies?${nextParams.toString()}`}
                  className="rounded-md border border-slate-600 px-3 py-1 text-xs font-medium text-slate-200 hover:border-sky-400 hover:text-sky-200"
                >
                  Next →
                </Link>
              )}
            </div>
          </div>
        </>
      )}
    </section>
  );
}



// MOVIE LIST PAGE //
