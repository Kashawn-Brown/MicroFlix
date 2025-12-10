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
  - `PUT /api/v1/ratings/movie/{movieId}/me`
    - Body includes a rating value (1.0–10.0)
    - Stored as `rating_times_ten` integer (10–100) to avoid floating-point issues

- **Get the current user’s rating**:
  - `GET /api/v1/ratings/movie/{movieId}/me`

- **Delete the current user’s rating**:
  - `DELETE /api/v1/ratings/movie/{movieId}/me`

- **Rating summary for a movie**:
  - `GET /api/v1/ratings/movie/{movieId}/summary`
    - Returns average rating and total count across all users
    - Backed by a JPA projection and a grouped query on `rating_times_ten`

- **List all ratings for the current user**:
  - `GET /api/v1/ratings/me`
    - Used by the “My ratings” page in the frontend

### Watchlist (engagements)

A generic `Engagement` entity is used so more engagement types can be added later:

- **Add to watchlist**:
  - `POST /api/v1/engagements/watchlist/{movieId}`
- **Remove from watchlist**:
  - `DELETE /api/v1/engagements/watchlist/{movieId}`
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
* OpenAPI JSON:

  * `GET /v3/api-docs`
* Swagger UI:

  * `GET /swagger-ui/index.html`

Locally: `http://localhost:8084/swagger-ui/index.html`
In production: accessible from the internal network for debugging and documentation.

---