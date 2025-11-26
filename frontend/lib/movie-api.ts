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

/**
 * Fetch a page of movies from the movie-service via the gateway.
 * For now just support page + size; add filters later.
 */
export async function fetchMoviesPage(page = 0, size = 12): Promise<Page<Movie>> {

  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  return apiFetch<Page<Movie>>(`/movie-service/api/v1/movies?${params}`);
}

/**
 * Fetch a single movie by id from the movie-service via the gateway.
 * Used by the movie details page.
 */
export async function fetchMovieById(id: number): Promise<Movie> {
  return apiFetch<Movie>(`/movie-service/api/v1/movies/${id}`);
}
