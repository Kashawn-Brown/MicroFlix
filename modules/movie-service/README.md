# Movie Service

The `movie-service` manages the **movie catalog**:

- Stores movie metadata (title, overview, release year)
- Manages genres and many-to-many movie-genre relationships
- Exposes search/filter/sort with pagination
- Seeds data from TMDb into a local Postgres database

It’s built with Spring Boot 3, Spring Data JPA, and PostgreSQL.

---

## Responsibilities

- **Core movie data**
  - `GET /api/v1/movies/{id}` – get a single movie by id
- **Search & browse**
  - `GET /api/v1/movies` – paginated search
    - `query` – free-text search on title
    - `genre` – filter by genre id
    - `year` – filter by release year
    - `sort` – one of:
      - `created_desc` (default, newest first)
      - `created_asc`
      - `title_asc`, `title_desc`
      - `year_asc`, `year_desc`
    - `page` – zero-based page index
    - `size` – page size (e.g., 20)
- **Genres**
  - `GET /api/v1/movies/genres` – list available genres for filter dropdowns

Responses are mapped to DTOs (e.g., `MovieResponse`) and include genres and poster/backdrop paths so the frontend doesn’t need to know the underlying schema.

---

## Data model & indexing

- **Entities**
  - `Movie` – main aggregate (title, overview, releaseYear, createdAt, posterPath, backdropPath, etc.)
  - `Genre` – genre lookup table
  - `MovieGenre` – explicit join entity between movies and genres

- **Flyway migrations** create:
  - `movies`, `genres`, `movie_genres` tables
  - targeted **indexes** on hot query paths, for example:
    - `created_at, id` (for `created_desc` pagination)
    - `release_year` (for year filter)
    - `movie_genres(genre_id, movie_id)` (for genre filtering)

Movie search was benchmarked under load with tools like `autocannon`, and these indexes were validated using `EXPLAIN ANALYZE` to keep high-percentile latency stable while increasing throughput.

---
## TMDb integration

Movie data is now seeded and enriched by the separate **tmdb-ingestion-service**.

`movie-service` itself:

- Stores the TMDb id (`tmdbId`) for each movie.
- Exposes internal endpoints that the ingestion job calls to:
  - Check if a movie already exists by `tmdbId`.
  - Create new movies.
  - Find movies that still need enrichment (e.g. missing runtime).
  - Apply partial updates (PATCH) to add runtime and, later, other fields.

All TMDb communication (popular/top rated/now playing/upcoming/discover lists and individual movie detail calls) happens inside `tmdb-ingestion-service`.

## Internal integration endpoints

These endpoints are not exposed through the gateway; they are used by internal jobs like `tmdb-ingestion-service`:

- `GET /api/internal/v1/movies/exists-by-tmdb/{tmdbId}`  
  Returns whether a movie with this TMDb id already exists (used for idempotent seeding).

- `GET /api/internal/v1/movies/needs-runtime`  
  Returns movies that have a `tmdbId` but no `runtime` yet.  
  Supports `page` and `size` query parameters for simple pagination.

- `PATCH /api/internal/v1/movies/{id}`  
  Partially updates a movie by its internal id using `UpdateMovieRequest`. Only non-null fields are applied.

- `PATCH /api/internal/v1/movies/by-tmdb/{tmdbId}`  
  Partially updates a movie by its TMDb id. Used when enriching movies that were just seeded.

---

## Running locally

Use the root Docker setup:

```bash
cd docker
docker compose up --build
````

This starts `movie-service` on port **8083** with its own Postgres database (`movie-db`).

To run only `movie-service`:

```bash
cd modules/movie-service
mvn spring-boot:run
```

Configure DB connection via `application.yml` or environment variables.

---

## Error handling

`movie-service` uses `MovieErrorAdvice` with `ProblemDetail`:

* `MovieNotFoundException` → **404 Movie not found**
* `IllegalArgumentException` → **400 Invalid movie request**
* `Exception` (fallback) → **500 Internal Error** with a generic message

All errors are returned as `application/problem+json` so the frontend can treat them consistently with the other services.

---

## Observability & API docs

* Health:

  * `GET /actuator/health`
* OpenAPI JSON:

  * `GET /v3/api-docs`
* Swagger UI:

  * `GET /swagger-ui/index.html`

Locally: `http://localhost:8083/swagger-ui/index.html`
In production: available on the internal network for debugging and documentation.

---

