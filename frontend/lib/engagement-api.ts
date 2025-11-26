// Handle backend engagements routes

import { apiFetch } from "./api-client";

// Expected Engagement Item Response
export type EngagementItem = {
  userId: string;
  movieId: number;
  type: string; // e.g. "WATCHLIST"
  addedAt: string;
};

/**
 * Fetch the current user's watchlist from the rating-service.
 * Requires a valid JWT token.
 */
export async function fetchWatchlist(
  token: string
): Promise<EngagementItem[]> {
  return apiFetch<EngagementItem[]>(`/rating-service/api/v1/engagements/watchlist`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

/**
 * Check if a specific movie is in the current user's watchlist.
 * Uses the dedicated boolean endpoint.
 */
export async function fetchInWatchlist(
  movieId: number,
  token: string
): Promise<boolean> {
    
  return apiFetch<boolean>(
    `/rating-service/api/v1/engagements/watchlist/${movieId}/me`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
}

/**
 * Add a movie to the current user's watchlist.
 * Idempotent on the backend.
 */
export async function addToWatchlist(
  movieId: number,
  token: string
): Promise<void> {
  await apiFetch<void>(
    `/rating-service/api/v1/engagements/watchlist/${movieId}`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
}

/**
 * Remove a movie from the current user's watchlist.
 * Idempotent on the backend.
 */
export async function removeFromWatchlist(
  movieId: number,
  token: string
): Promise<void> {
  await apiFetch<void>(
    `/rating-service/api/v1/engagements/watchlist/${movieId}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
}
