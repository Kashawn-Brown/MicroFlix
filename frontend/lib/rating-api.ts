// lib/rating-api.ts
import { apiFetch } from "./api-client";

export type RatingSummary = {
  movieId: number;
  average: number | null;
  count: number;
};

/**
 * Fetch the public rating summary for a movie from the rating-service.
 * Does not require auth; used on the movie details page.
 */
export async function fetchRatingSummary(
  movieId: number
): Promise<RatingSummary> {
  return apiFetch<RatingSummary>(
    `/rating-service/api/v1/ratings/movie/${movieId}/summary`
  );
}
