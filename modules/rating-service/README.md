# Rating Service

The `rating-service` is responsible for **user engagement with movies**:

- Per-user per-movie ratings on a 1–10 scale (0.1 increments)
- Rating summaries (average + count)
- A flexible `engagements` table used to implement a watchlist (and future favorites/likes)

It’s built with Spring Boot 3, Spring Data JPA, and PostgreSQL.

---

## Responsibilities

### Ratings

- **Upsert rating** for the current user:
  - `POST /api/v1/ratings` with body `{ "movieId": number, "rate": number }`
    - `rate` is a value between 1.0 and 10.0 (0.1 increments)
    - Stored as `rating_times_ten` integer (10–100) to avoid floating-point issues
    - Idempotent on `(userId, movieId)` — a second call with a new `rate` updates the existing row

- **Get the current user's rating for a specific movie**:
  - `GET /api/v1/ratings/movie/{movieId}/me`

- **Delete the current user's rating for a specific movie**:
  - `DELETE /api/v1/ratings/{movieId}`
    - Path param is the **movieId**, not a rating id — the service resolves the row from `(currentUserId, movieId)`

- **Rating summary for a movie**:
  - `GET /api/v1/ratings/movie/{movieId}/summary`
    - Returns average rating and total count across all users
    - Backed by a JPA projection and a grouped query on `rating_times_ten`

- **List all ratings for the current user**:
  - `GET /api/v1/ratings/me`
    - Used by the "My ratings" page in the frontend

### Watchlist (engagements)

A generic `Engagement` entity is used so more engagement types can be added later:

- **Add to watchlist** (idempotent):
  - `PUT /api/v1/engagements/watchlist/{movieId}` → 204
- **Remove from watchlist** (idempotent):
  - `DELETE /api/v1/engagements/watchlist/{movieId}` → 204
- **List watchlist items**:
  - `GET /api/v1/engagements/watchlist`
- **Check if a movie is on watchlist**:
  - `GET /api/v1/engagements/watchlist/{movieId}/me` → `true`/`false`

A unique constraint on `(user_id, movie_id, type)` ensures idempotent add/remove semantics.

---

## Data model

- `Rating`
  - `userId`, `movieId`
  - `ratingTimesTen` integer (10–100 for ratings 1.0–10.0)
  - timestamps
- `Engagement`
  - `userId`, `movieId`
  - `type` (e.g. `WATCHLIST`)
  - `createdAt`

Indexes cover hot paths such as `(movie_id)` for summary queries and `(user_id, type)` for user watchlists.

---

## Running locally

Via root Docker:

```bash
cd docker
docker compose up --build
````

This starts `rating-service` on port **8084** with a dedicated Postgres database (`rating-db`).

To run only `rating-service`:

```bash
cd modules/rating-service
mvn spring-boot:run
```

Configure DB connection via `application.yml` or environment variables.

---

## Security & JWT

`rating-service` validates JWTs locally using the same shared secret and claims convention as `user-service`.
Controllers can rely on a “current user” abstraction (e.g., a custom principal) rather than re-parsing tokens.

---

## Error handling

`RatingErrorAdvice` maps exceptions to `ProblemDetail`:

* `RatingNotFoundException` → **404 Rating not found**
* `IllegalArgumentException` → **400 Invalid rating request**
* `Exception` (fallback) → **500 Internal Error** with a safe message

All errors are returned as `application/problem+json` to keep behavior consistent with other services and the gateway.

---

## Observability & API docs

* Health:

  * `GET /actuator/health`
* Prometheus metrics scrape (HTTP server with latency histograms, JVM, HikariCP pool):

  * `GET /actuator/prometheus`
* OpenAPI JSON:

  * `GET /v3/api-docs`
* Swagger UI:

  * `GET /swagger-ui/index.html`

Locally: `http://localhost:8084/swagger-ui/index.html`
In production: accessible from the internal network for debugging and documentation.

This service appears in the **MicroFlix Overview** Grafana dashboard at `http://localhost:3001` (request rate, latency percentiles, status codes, JVM heap, HikariCP).

---

## Performance notes

Rating-service is a participant in both gateway aggregation endpoints (`/api/v1/catalog/movies/{id}` and `/api/v1/catalog/watchlist`). The Branch 3 frontend migration replaces direct browser calls against `/ratings/movie/{id}/me`, `/ratings/movie/{id}/summary`, and the `/engagements/watchlist/*` paths with server-side fan-out from the gateway. Before/after page-load numbers, load scripts, and methodology live in [`docs/benchmarks.md`](../../docs/benchmarks.md).

---