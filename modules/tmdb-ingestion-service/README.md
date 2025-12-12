# TMDb Ingestion Service

The `tmdb-ingestion-service` is a **one-shot batch job** that pulls movies from **TMDb** and seeds them into `movie-service` via HTTP.

Unlike the other Spring Boot apps, this service does **not** expose an HTTP API. It runs a `CommandLineRunner` once, inserts movies, then exits. It’s designed to be triggered **manually (for now) or on a schedule** in both local and EC2 environments. 

---

## Responsibilities

- Call TMDb list endpoints (popular, top rated, now playing, upcoming, plus a `/discover`-based feed).
- Map TMDb results into the same `CreateMovieRequest` DTO used by `movie-service`.
- Deduplicate by TMDb id:
  - Skip movies with missing `tmdbId`.
  - Call `movie-service`’s internal `GET /api/internal/v1/movies/exists-by-tmdb/{tmdbId}` to avoid inserting duplicates.
- Insert new movies via `POST /api/v1/movies` on `movie-service`.
- **Optionally enrich movies after seeding**:
  - For movies created in this run (`--enrich`), fetch TMDb detail and PATCH fields like `runtime`.
  - For existing movies missing runtime (`--enrich-runtime`), fetch candidates from `movie-service` and backfill their runtime.
- Respect a target count and pagination limits so the job:
  - Stops when the target count is reached,
  - Or when a page yields no new movies,
  - Or when the configured maximum TMDb page limit is hit.

This keeps the ingestion job **idempotent** and safe to re-run without duplicating data in the movie catalog.

---

## Configuration

Configuration is driven by **Spring Boot properties + environment variables**.

### TMDb configuration

```yaml
tmdb:
  base-url: https://api.themoviedb.org/3
  api-key: ${TMDB_API_KEY}
````

Environment:

* `TMDB_API_KEY` – **required**. TMDb API key used by the ingestion job.

### Movie-service integration

```yaml
movie-service:
  base-url: ${MOVIE_SERVICE_BASE_URL:http://localhost:8083}
```

Environment:

* `MOVIE_SERVICE_BASE_URL`

    * Local default: `http://localhost:8083`
    * In Docker / EC2: typically `http://movie-service:8083`

The ingestion service uses this base URL to call:

- `GET /api/internal/v1/movies/exists-by-tmdb/{tmdbId}` – internal existence check used during seeding.
- `POST /api/v1/movies` – create movie in the catalog.
- `GET /api/internal/v1/movies/needs-runtime` – fetch movies that have a `tmdbId` but no `runtime` yet (runtime backfill mode).
- `PATCH /api/internal/v1/movies/{id}` – partial update by internal id (used for generic backfill).
- `PATCH /api/internal/v1/movies/by-tmdb/{tmdbId}` – partial update by TMDb id (used when enriching movies created in this run).

### Ingestion tuning

```yaml
ingestion:
  default-count: 50   # fallback when no --count=NN is passed
  max-pages: 50       # safety cap on how many TMDb pages to scan
```

* **`default-count`** – how many movies the job will try to insert when you don’t pass any CLI arguments.
* **`max-pages`** – upper bound on TMDb pagination to avoid runaway loops.

At runtime:

* If you run with **no args**, the job uses `ingestion.default-count` (e.g., 50).
* If you run with `--count=100`, that value overrides the default.

---

## Running locally

### 1. From Maven (no Docker)

From the repo root:

```bash
cd modules/tmdb-ingestion-service

# Seed up to 50 new movies
mvn spring-boot:run -Dspring-boot.run.arguments="--count=50"

# Use default ingestion.default-count from application.yml
mvn spring-boot:run

# Seed and then enrich only the movies created in this run
mvn spring-boot:run -Dspring-boot.run.arguments="--count=50 --enrich"

# Backfill runtimes for existing movies that are missing runtime
mvn spring-boot:run -Dspring-boot.run.arguments="--enrich-runtime --update-limit=200"
```

Make sure `movie-service` is running on `http://localhost:8083` and `TMDB_API_KEY` is set in your environment.

---

### 2. Via Docker Compose (local stack)

The ingestion service is defined in `docker/docker-compose.yml` alongside the other services, but marked as a **job** rather than a long-running API.

To bring up the main stack (discovery, gateway, user-service, movie-service, rating-service, frontend, databases):

```bash
cd docker
docker compose up --build
```

By default, `tmdb-ingestion-service` is **not** started automatically; it’s meant to be invoked on demand.

To run the ingestion job as a one-off container (using the `jobs` profile):

```bash
cd docker
docker compose --profile jobs run --rm tmdb-ingestion-service --count=50
```
To seed and then enrich only the movies created in this run:

```bash
docker compose --profile jobs run --rm tmdb-ingestion-service --count=50 --enrich
````

To backfill runtime for existing movies that are missing it:

```bash
docker compose --profile jobs run --rm tmdb-ingestion-service --enrich-runtime --update-limit=200
```

* Omitting `--count` makes it fall back to `ingestion.default-count` from `application.yml`.

The service picks up `TMDB_API_KEY` and `MOVIE_SERVICE_BASE_URL` from `.env` just like the other services.

### Updating the job after code changes

The ingestion job runs inside a Docker image. If you change Java code or configuration in
`tmdb-ingestion-service`, you **must rebuild the image** before the next run:

```bash
cd docker

# Rebuild only the tmdb-ingestion-service image
docker compose build tmdb-ingestion-service

# Then run the job using the fresh image
docker compose --profile jobs run --rm tmdb-ingestion-service --count=10
````

If you skip the `build` step, `docker compose run` will use the **previous image** and you’ll be
running old code, even though the source files have changed.
---

## Running in production (AWS EC2)

In EC2, the main stack is brought up with:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

This starts:

* `discovery`, `gateway`, `user-service`, `movie-service`, `rating-service`
* Postgres containers (`user-db`, `movie-db`, `rating-db`)
* `frontend` (exposed on port 80)

The ingestion service is present in the compose file but **only runs when explicitly invoked**:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml \
  --profile jobs run --rm tmdb-ingestion-service --count=50
```

To seed and enrich newly created movies in one run:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml \
  --profile jobs run --rm tmdb-ingestion-service --count=50 --enrich
````

To backfill runtime for older movies:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml \
  --profile jobs run --rm tmdb-ingestion-service --enrich-runtime --update-limit=200
```

This command can later be wired into a **cron job**, a scheduled GitHub Action, or a manual run whenever you want to top up the catalog with more TMDb movies.

### Updating the job after code changes (production / EC2)

In production, the ingestion job also runs inside a Docker image and is wired through
`docker-compose.yml` + `docker-compose.prod.yml`.

Any time you change code in `tmdb-ingestion-service` and deploy those changes to EC2,
you should:

```bash
cd docker

# Rebuild only the tmdb-ingestion-service image with the prod config in mind
docker-compose -f docker-compose.yml -f docker-compose.prod.yml \
  --profile jobs build tmdb-ingestion-service

# Then run the job using that fresh image
docker-compose -f docker-compose.yml -f docker-compose.prod.yml \
  --profile jobs run --rm tmdb-ingestion-service --count=50
````

If you skip the `build` step, Docker will reuse the previously built image and the job
will run the old code.
---

## Error handling

The ingestion service:

* Logs per-page progress and skip reasons (missing `tmdbId`, already in DB).
* Relies on `movie-service`’s existing `ProblemDetail`-based error handling for internal endpoints:

    * If `movie-service` returns an error (e.g., 500), the job will fail fast with a clear log message to avoid silently dropping data.

Because this is a batch job and not a public API, failures are surfaced through logs and exit code rather than an HTTP response.

---

