// Talks to the gateway's aggregation endpoints under /api/v1/catalog/*.
// Used by the movie-detail page (SSR: anonymous) and MovieActions (CSR: authed).

import { apiFetch } from "./api-client";
import type { Movie } from "./movie-api";
import type { RatingSummary } from "./rating-api";

// Current user's slice of the aggregated response.
// Anonymous requests get { rating: null, inWatchlist: false }.
export type CatalogMeSection = {
  rating: number | null;
  inWatchlist: boolean;
};

// Full aggregate: movie metadata + public rating summary + per-user me slice.
// Mirrors the gateway's CatalogMovieDetailsResponse record.
export type CatalogMovieDetails = {
  movie: Movie;
  ratingSummary: RatingSummary;
  me: CatalogMeSection;
};

/**
 * Anonymous fetch of the aggregated movie-detail payload. SSR path — no token,
 * so me short-circuits server-side to the anonymous view. One round-trip.
 */
export async function fetchCatalogMovieDetails(
  movieId: number
): Promise<CatalogMovieDetails> {
  return apiFetch<CatalogMovieDetails>(`/api/v1/catalog/movies/${movieId}`);
}

/**
 * Authed fetch of the same aggregate. CSR path — caller forwards the JWT so the
 * gateway fills the me section. MovieActions only consumes me.* and ignores the
 * movie + ratingSummary (already rendered by SSR).
 */
export async function fetchCatalogMovieDetailsAuthed(
  movieId: number,
  token: string
): Promise<CatalogMovieDetails> {
  return apiFetch<CatalogMovieDetails>(`/api/v1/catalog/movies/${movieId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}
