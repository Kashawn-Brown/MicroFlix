// Handle backend ratings routes

import { apiFetch, ApiError } from "./api-client";

// Rating Summary format
export type RatingSummary = {
  movieId: number;
  average: number | null;
  count: number;
};


// Expected Rating response format
export type RatingResponse = {
  id: number;
  userId: string;
  movieId: number;
  rate: number;
  createdAt: string;
  updatedAt: string;
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

/**
 * Fetch all ratings for the current user from the rating-service.
 * Requires a valid JWT token.
 */
export async function fetchMyRatings(
  token: string
): Promise<RatingResponse[]> {
  return apiFetch<RatingResponse[]>(`/rating-service/api/v1/ratings/me`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

/**
 * Fetch the current user's rating for a specific movie from the rating-service.
 * Requires a valid JWT token.
 */
export async function fetchMyRatingForMovie(
  movieId: number,
  token: string
): Promise<RatingResponse | null> {
  try {
    return await apiFetch<RatingResponse>(
      `/rating-service/api/v1/ratings/movie/${movieId}/me`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      // No rating yet for this movie → not an error for the UI.
      return null;
    }
    throw error;
  }
}

/**
 * Create or update the current user's rating for a movie.
 * Uses the POST /ratings endpoint, which upserts based on (userId, movieId).
 */
export async function upsertMyRating(
  movieId: number,
  rate: number,
  token: string
): Promise<RatingResponse> {
  return apiFetch<RatingResponse>(`/rating-service/api/v1/ratings`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ movieId, rate }),
  });
}

/**
 * Delete the current user's rating for a movie.
 */
export async function deleteMyRating(
  movieId: number,
  token: string
): Promise<void> {
  await apiFetch<void>(`/rating-service/api/v1/ratings/${movieId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}