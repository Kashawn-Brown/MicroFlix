# Microflix Frontend

This is the **Next.js frontend** for Microflix – the movie platform I’m building on top of a Spring Boot microservices backend (user-service, movie-service, rating-service, gateway, discovery).

The goal is to have a small but real web app that exercises the backend in a production-ish way: auth, catalog browsing, ratings, and a watchlist, all wired through the gateway.

---

## Stack & philosophy

* **Framework:** Next.js (App Router) with TypeScript
* **Styling:** Tailwind CSS
* **Runtime:** React client components where needed (auth, forms, interactive bits)
* **Backend:** Talks to the Spring Cloud Gateway (`http://localhost:8081` by default), which routes to:

  * `user-service` for auth/profile
  * `movie-service` for movies & genres
  * `rating-service` for ratings + watchlist

Design goals:

* Keep the UI simple and clean, not overengineered.
* Show realistic patterns: JWT auth, typed API client, error handling, pagination, filters.
* Mirror the backend’s structure: clear separation between API helpers, layout, and pages.

---

## Prerequisites

To run the frontend as intended, I assume:

* **Node.js** 20+ (LTS is fine)
* **npm** or **pnpm** (examples use `npm`)
* The **Microflix backend stack is running** via Docker Compose, with:

  * Gateway on `http://localhost:8081`
  * User, movie, and rating services registered with Eureka and reachable through the gateway

Backend is started from the repo root:

```bash
cd docker
docker compose up --build
```

---

## Configuration

The frontend talks to the backend through a single base URL. I keep this in an env var so it’s easy to point at different environments.

Create a `.env.local` in the `frontend/` directory:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
```

If this is missing, the API client may default to `http://localhost:8081`, but I treat the env var as the source of truth. Check `lib/api-client.ts` if you want to see exactly how it’s wired.

---

## Running the frontend

From the `frontend/` directory:

```bash
# Install dependencies
npm install

# Start dev server
npm run dev
```

By default, the app will be available at:

```text
http://localhost:3000
```

The backend (gateway + services) should be running on `http://localhost:8081` at the same time, or API calls will fail.

---

## Project structure (high-level)

The frontend uses the Next.js App Router, no `src/` folder – everything lives under `app/`.

Rough structure:

* `app/layout.tsx`
  Root layout: global `<html>` / `<body>`, top navigation bar, and a main content container.

* `app/page.tsx`
  Home page with a hero, a featured movie, and a “recently added” row.

* `app/movies/page.tsx`
  Server-side movies listing page with search, filters, sort, pagination, and page-size selector.

* `app/movies/[id]/page.tsx`
  Movie details page (poster/backdrop, title, year, overview, genres) plus rating summary and user actions.

* `app/watchlist/page.tsx`
  Client page that loads the current user’s watchlist via rating-service and displays it as a grid of movies.

* `app/profile/page.tsx`
  Profile overview (basic user info + simple stats like count of ratings/watchlist items).

* `app/profile/ratings/page.tsx` (or similar)
  “My Ratings” page; shows every movie the user has rated, joined with movie details.

* `app/login/...`
  Combined login/register experience (using query param or toggle), wired to user-service auth endpoints.

* `app/lib/`

  * `auth-storage.ts`: manages storing/loading/clearing auth info in `localStorage` and broadcasts an auth-changed event.
  * `auth-header.tsx`: small client component that renders login/register links or “Welcome, <name> / Logout” in the header.
  * `api-client.ts`: low-level helper for `fetch` calls to the gateway, including `ApiError` wrapping.
  * `movie-api.ts`, `rating-api.ts`, `engagement-api.ts`, `auth-api.ts`: typed wrappers around specific backend endpoints.

This is intentionally small and direct. Each page depends on the relevant API helpers instead of inlining fetch calls everywhere.

---

## Core screens & flows

### 1. Layout & header

The layout sets up:

* Dark background, centered content with a max width (e.g. `max-w-6xl`).
* A top navigation bar with:

  * Brand (“MicroFlix”) and optional logo.
  * Nav links: **Movies**, **Watchlist**.
  * Right-hand **AuthHeader** that changes based on whether the user is logged in.

`AuthHeader`:

* Looks up auth data via `loadAuth()` (display name, email, token).
* If **not** logged in:

  * Shows **Login** and **Create account** links.
* If **logged in**:

  * Shows “Welcome, {displayName}” with the name linking to the profile page.
  * Shows a **Logout** button.

It also listens for a global `microflix-auth-changed` event (fired anytime auth is saved or cleared) so the header stays up-to-date when auto-logout happens elsewhere.

---

### 2. Auth (login & register)

The frontend uses the **user-service** auth endpoints via the gateway: 

* `POST /api/v1/auth/register`
* `POST /api/v1/auth/login`

On a successful login or registration:

1. The backend returns an auth payload including:

   * JWT `token`
   * `email`
   * `displayName`
2. The frontend calls `saveAuth({ token, email, displayName })`, which:

   * Saves the auth object to `localStorage` under a single key.
   * Dispatches a `microflix-auth-changed` event for listeners (like `AuthHeader`).

On logout:

* `clearAuth()` removes the entry from `localStorage` and also dispatches `microflix-auth-changed`.

Handling expired tokens:

* When a page like the movie actions component or watchlist page calls a protected endpoint and gets `401 Unauthorized` (wrapped as an `ApiError`), it:

  * Calls `clearAuth()` to wipe local auth,
  * Updates local state to treat the user as logged out,
  * Optionally redirects to `/login`.

This keeps the frontend and backend consistent around “I had a token, but the backend says it’s no longer valid.”

---

### 3. Movies list (`/movies`)

This page is responsible for the main catalog browsing experience.

**Data source:** `GET /movie-service/api/v1/movies` through the gateway. Under the hood this maps to the movie-service search endpoint (which supports filters, sort, and pagination). 

Supported query params (all passed through via the URL):

* `query` – free-text search on title (case-insensitive substring).
* `genre` – exact genre name (e.g. `"Action"`, `"Comedy"`).
* `year` – release year (e.g. `2010`).
* `sort` – one of:

  * `created_desc` (default)
  * `created_asc`
  * `title_asc` / `title_desc`
  * `year_asc` / `year_desc`
* `page` – zero-based page index.
* `size` – page size (user-selectable).

The page:

* Uses a **GET form** at the top for filters:

  * Search input (`query`).
  * Genre dropdown:

    * The options are fetched from the backend via `GET /movie-service/api/v1/movies/genres` (returns a list of `GenreResponse { id, name }`).
    * The UI shows “Any Genre” plus those names.
  * Year input (`year`).
  * Sort select (`sort`).
  * **Reset** button that links back to `/movies` with no query params.
  * **Apply** button that submits the form via GET, so the URL always reflects the current filters.

* Uses the backend’s `Page<MovieResponse>` JSON shape to:

  * Render a grid of movie cards (poster, title, year, genres).
  * Show a footer with:

    * “Page X of Y”
    * Previous/Next buttons
    * A set of **numbered page links** with ellipses when there are many pages.
    * A **Page size** selector (e.g. 15, 20, 25, 30); changing size resets to page 0.

* Handles:

  * Empty result sets (nice “No movies found. Try adjusting your filters.” message).
  * Errors from the backend using the `ProblemDetail` info if available.

---

### 4. Movie details (`/movies/[id]`)

The movie details page does a couple of things:

* **Fetches one movie** via `GET /movie-service/api/v1/movies/{id}`.
* Shows:

  * Poster (or fallback if missing).
  * Title + release year.
  * Genres.
  * Overview.
* **Fetches rating summary** via the rating-service:

  * `GET /rating-service/api/v1/ratings/movie/{movieId}/summary`
  * This returns:

    * `movieId`
    * `average` (e.g. `8.1` or `null` if no ratings)
    * `count` (number of ratings). 

The UI renders something like:

> ⭐ 8.1 / 10 (based on 42 ratings)

or a friendly “No ratings yet” message.

Below the summary, there’s a **MovieActions** client component for logged-in users.

---

### 5. Movie actions: rating + watchlist (logged-in users)

The movie actions component handles all interactive user-specific behavior for a single movie.

On mount, if the user is logged in (token exists):

1. It loads:

   * The user’s **own rating for this movie** via a dedicated endpoint:

     * `GET /rating-service/api/v1/ratings/movie/{movieId}/me`
   * Whether the movie is in the user’s **watchlist** via:

     * `GET /engagements/watchlist/{movieId}/me` (returns a boolean).

2. It then shows:

   * A rating panel:

     * If you have a rating: shows “Current: X/10” and a number input prefilled with your rating.
     * If you have no rating: empty input.
   * A watchlist panel:

     * Text explaining whether this movie **is / isn’t** on your watchlist.
     * A button to **Add to watchlist** or **Remove from watchlist**.

**Rating endpoints used:**

* **Upsert (create or update) rating:**
  `POST /rating-service/api/v1/ratings` with `{ "movieId": number, "rate": number }`.
* **Delete rating:**
  `DELETE /rating-service/api/v1/ratings/movie/{movieId}`.

The frontend keeps the behavior:

* Validates the rating is between `1.0` and `10.0` before sending.
* Displays backend validation errors using `ProblemDetail` when the rating is invalid.

**Watchlist endpoints used:** 

* **Add to watchlist:**
  `PUT /rating-service/api/v1/engagements/watchlist/{movieId}`
* **Remove from watchlist:**
  `DELETE /rating-service/api/v1/engagements/watchlist/{movieId}`

All of these calls send the JWT as:

```http
Authorization: Bearer <JWT>
```

If any of them return `401`, the component auto-clears auth and redirects to `/login`.

---

### 6. Watchlist page (`/watchlist`)

The watchlist page is a client page that:

* Reads auth from `localStorage` via `loadAuth()`.
* If there’s **no token**, shows a “Sign in to view your watchlist” message with a link to `/login`.
* If logged in:

  * Calls `GET /rating-service/api/v1/engagements/watchlist` with the JWT.
  * For each engagement (`{ userId, movieId, type, addedAt }`), fetches the movie details from movie-service.
  * Joins them into a list of movies-with-addedAt and displays them in a grid similar to the main Movies page.
  * Each card includes:

    * Poster, title, year, genres.
    * A “Remove from watchlist” button that calls the DELETE endpoint and updates UI optimistically.

---

### 7. Profile & “My ratings”

To make the app feel more like a real account-based product, there’s a simple profile area.

**Profile page (`/profile`):**

* Uses the user-service endpoint `GET /api/v1/users/me` (with JWT) to show:

  * Email
  * Display name
* May show a small summary (e.g. how many ratings you’ve created, watchlist count), using the relevant rating-service endpoints.

**My ratings page (`/profile/ratings` or similar):**

* Reads auth from `localStorage`.
* If not logged in, shows the familiar “Sign in to see your ratings” CTA.
* If logged in:

  1. Calls `GET /rating-service/api/v1/ratings/me` to get all ratings for the current user.
  2. For each distinct movieId, calls `fetchMovieById(movieId)` to get movie metadata.
  3. Joins them into an array of `{ rating, movie }` and shows them as cards.

Each card includes:

* Poster, title, year.
* “Your rating: X/10” where:

  * `10.0 → "10/10"`
  * `9.0 → "9/10"`
  * `8.5 → "8.5/10"`
* Buttons to:

  * **View details** (links to `/movies/{id}`).
  * **Remove rating** (calls the DELETE endpoint and updates state).

This page is a good example of how the frontend composes data from multiple microservices into a user-centric view.

---

## API client & error handling

All HTTP calls go through a typed `apiFetch<T>()` helper that:

* Prepends `NEXT_PUBLIC_API_BASE_URL`.
* Adds `Authorization: Bearer <token>` when a token is provided.
* Parses JSON responses.
* When `response.ok` is false:

  * Attempts to parse the body as a `ProblemDetail` (`type`, `title`, `status`, `detail`, etc.).
  * Throws an `ApiError` with:

    * `status`
    * `problem` (if available)
    * generic fallback message otherwise.

Components then handle errors like:

```ts
try {
  const data = await someApiCall();
} catch (error) {
  if (error instanceof ApiError) {
    const detail =
      error.problem?.detail ||
      error.problem?.title ||
      "Failed to load XYZ.";
    setErrorMessage(detail);
  } else {
    setErrorMessage("Failed to load XYZ.");
  }
}
```

This keeps the UI logic clean and gives a single place to refine networking behavior later.

---

## Notes & future improvements

The frontend is intentionally **MVP-sized** but already in a good place for a portfolio:

* Uses a real microservices backend with JWT, rating logic, and watchlist.
* Has realistic flows: browsing, filtering, rating, and tracking movies.
* Treats errors and auth expiry in a reasonably robust way.

Ideas for future iterations:

* Route guards / redirects for protected pages (e.g. auto-redirect `/watchlist` to `/login?from=watchlist` if not authed).
* More refined empty states and success toasts (e.g. “Added to watchlist”).
* Visual polish: skeleton loaders, hover effects, and a little more responsive tuning.
* Stronger metadata/SEO once I’m ready to deploy the app publicly.

For now:

1. Start the backend stack via Docker.
2. Start the frontend with `npm run dev`.
3. Understand how the main pages work and how they map to backend endpoints.
4. Confidently use this project as a demo of full-stack, microservice-aware development.
