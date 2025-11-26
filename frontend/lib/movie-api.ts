// Handle backend movie routes

import { apiFetch } from "./api-client";

// Expected movie response
export type Movie = {
  id: number;
  title: string;
  overview?: string | null;
  releaseYear?: number | null;
  posterUrl?: string | null;
  backdropUrl?: string | null;
  genres: string[];
};

// Reusable generic Page data type for paginated responses
export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page index (0-based)
  size: number;   // page size
};

// Flexible search/filter options for retrieving movies
export type MovieSearchOptions = {
  page?: number;
  size?: number;
  query?: string;
  genre?: string;
  year?: number;
  sort?: string;
};

/**
 * Fetch a page of movies from the movie-service via the gateway.
 * Support page + size + filters & sorting+.
 */
export async function fetchMoviesPage(options: MovieSearchOptions = {}): Promise<Page<Movie>> {

  // destructure options (and give defaults where needed)
  const {
    page = 0,
    size = 12,
    query,
    genre,
    year,
    sort
  } = options;
  
  // Start building the query parameters
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  //Conditionally adding filters + sorting (Only include if present and non-empty)
  if (query && query.trim().length > 0) {
    params.set("query", query.trim());
  }
  if (genre && genre.trim().length > 0) {
    params.set("genre", genre.trim());
  }
  if (typeof year === "number" && !Number.isNaN(year)) {
    params.set("year", String(year));
  }
  if (sort && sort.trim().length > 0) {
    params.set("sort", sort.trim());
  }

  return apiFetch<Page<Movie>>(
    `/movie-service/api/v1/movies?${params.toString()}`
  );

}

/**
 * Fetch a single movie by id from the movie-service via the gateway.
 * Used by the movie details page.
 */
export async function fetchMovieById(id: number): Promise<Movie> {
  return apiFetch<Movie>(`/movie-service/api/v1/movies/${id}`);
}
