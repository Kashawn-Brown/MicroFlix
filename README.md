# MicroFlix

MicroFlix is a microservices-based movie platform I built as deliberate practice in production-style backend architecture — Java, Spring Boot 3, Spring Cloud, real service boundaries, gateway routing, distributed authentication. The product is a movie catalog with per-user ratings and watchlists, backed by TMDb. The product gives the project a real domain to work in, but the architecture is what the project is actually about.

The system is split into separate Spring Boot services (users, movies, ratings, plus a one-shot TMDb ingestion job) behind a Spring Cloud Gateway, with a Next.js frontend on top and a Docker / AWS EC2 deployment that mirrors a real-world setup.

---

## Project status

The project was deployed to AWS EC2 with a real CI/CD pipeline through GitHub Actions. The deployment is currently retired — the always-on cost of running multiple JVMs and three Postgres instances on managed AWS doesn't make sense for a portfolio project with no real users. Everything in this README that describes the deployment reflects what was actually built and ran in production; you can also bring the full stack up locally with the instructions below.

---

## What this project demonstrates

This was built as deliberate practice, not as a product. What it demonstrates is end-to-end work on a real distributed backend system:

- Designing service boundaries along genuine data ownership lines (users, movies, ratings)
- Cross-service authentication via JWTs verified independently per service, no central auth round-trip
- API gateway with routing and parallel aggregation across multiple downstream services
- One database per data-owning service, coordinated via Docker Compose
- Standardized error handling across services using RFC 7807 `ProblemDetail`
- Multi-stage Docker builds, Docker Compose orchestration with separate dev and prod overrides
- End-to-end metrics pipeline with Micrometer, Prometheus, and provisioned Grafana dashboards
- Performance work driven by measurement: a meaningful TMDb-seeded catalog (~3.7k movies, ~8.4k movie–genre links), `EXPLAIN ANALYZE` against the hot query paths, targeted indexes added through Flyway, and explicit "considered but rejected" entries where adding an index wasn't worth its write cost
- Real CI/CD through GitHub Actions — CI builds and pushes images, CD pulls and deploys, with explicit ordering between them
- AWS EC2 deployment with real production debugging (browser/SSR/container networking, sizing, disk pressure)

---

## Architecture overview

**Modules:**

- `modules/user-service`  
  User registration, login (JWT), `/users/me`, profile update, and password change.

- `modules/movie-service`  
  Movie metadata (title, overview, year, genres, poster/backdrop) with search/filter/sort + pagination, TMDb-based seeding, and Flyway-managed indexes (V4 base coverage; V5 added trigram search and `tmdb_id` lookup; V6 promoted `tmdb_id` to UNIQUE).

- `modules/tmdb-ingestion-service`  
  One-shot Spring Boot job that seeds movies from TMDb into `movie-service` and can enrich them (e.g. runtime). Throttled per-call against TMDb's rate limit with retry-on-429. Two modes: full seeding (default) and `--mode=scheduled` (volatile endpoints + date-windowed `discover`) for cron-style top-ups.

- `modules/rating-service`  
  Movie ratings (1–10 scale with 0.1 increments, stored as `rating_times_ten`) plus a watchlist feature via a generic `engagements` table.

- `modules/gateway`  
  Spring Cloud Gateway entrypoint. Routes traffic to the microservices and exposes **aggregated catalog endpoints** for the frontend (e.g. `/api/v1/catalog/movies/{id}`).

- `modules/discovery`  
  Eureka discovery server so services can find each other by name (`lb://user-service`, `lb://movie-service`, etc.).

- `frontend/`  
  Next.js App Router app (TypeScript + Tailwind CSS) for auth, browsing movies, rating, and watchlist.

- `docker/`  
  Docker Compose setup for running the full stack locally and in AWS.

**Key technologies:**

- Java 21, Spring Boot 3.5, Spring Cloud 2025
- Spring Web, Spring Data JPA, Spring Security (JWT)
- Spring Cloud Gateway + Eureka Discovery
- PostgreSQL + Flyway migrations
- Next.js (App Router), TypeScript, React 19, Tailwind CSS
- Docker + Docker Compose
- Micrometer + Prometheus + Grafana (metrics + dashboards)
- JUnit 5, Mockito, Testcontainers
- GitHub Actions for CI + EC2 deploy

---

## Running the stack locally

### Prerequisites

- Java 21
- Node.js 20+
- Docker + Docker Compose
- (Optional) TMDb API key configured in environment for movie seeding

### Backend + frontend via Docker

From the root:

```bash
cd docker
docker compose up --build
```

This brings up:

- `discovery` (Eureka) on **http://localhost:8761**
- `gateway` on **http://localhost:8081**
- `user-service` on **http://localhost:8082**
- `movie-service` on **http://localhost:8083**
- `rating-service` on **http://localhost:8084**
- three Postgres instances (`user-db`, `movie-db`, `rating-db`)
- `frontend` on **http://localhost:80** (or `http://localhost:3000` in dev-only setups)
- `prometheus` on **http://localhost:9090**
- `grafana` on **http://localhost:3001** (admin/admin; MicroFlix Overview dashboard auto-provisioned)

The frontend talks only to the **gateway**, not directly to the microservices.

### TMDb ingestion jobs (manual)

The TMDb ingestion service is a **one-off job**, not a long-running API. You run it when you want to add or enrich movies.

From `docker/`:

```bash
# Make sure the main stack is running
docker compose up --build

# Seed up to 50 new movies from TMDb into movie-service
docker compose --profile jobs run --rm tmdb-ingestion-service --count=50

# Seed and then immediately enrich the movies created in this run (e.g. runtime)
docker compose --profile jobs run --rm tmdb-ingestion-service --count=50 --enrich

# Backfill runtimes for any existing movies missing runtime (generic enrichment)
docker compose --profile jobs run --rm tmdb-ingestion-service --enrich-runtime --update-limit=200

# Scheduled-mode top-up: skips popular/top_rated, uses date-windowed discover.
# This is the right shape for cron / scheduled GitHub Action runs.
docker compose --profile jobs run --rm tmdb-ingestion-service --mode=scheduled --count=50
```

The `--count` flag controls how many new movies to insert (0 or missing means "use default" from config). The `--update-limit` flag controls how many movies to update in backfill mode.

The `--mode=` flag selects the endpoint set: `full` (default) hits all five TMDb list endpoints; `scheduled` hits only `now_playing`, `upcoming`, and a date-windowed `discover`. See `modules/tmdb-ingestion-service/README.md` for details.

> **Note:** After changing code in `tmdb-ingestion-service`, run:
>
> ```bash
> docker compose build tmdb-ingestion-service
> ```
>
> before calling `docker compose --profile jobs run ...`, otherwise Docker will reuse the old image.

### Frontend in dev mode (optional)

If you want to run the frontend in dev mode while the backend runs via Docker:

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

Set `NEXT_PUBLIC_API_BASE_URL` to point at your gateway, e.g. `http://localhost:8081`.

---

## API & routing model

From the **frontend's perspective**, all API calls go through a `/gateway` prefix that Next.js rewrites to the gateway. For example:

- Browser calls:
  `GET /gateway/movie-service/api/v1/movies?query=inception`
- Next.js rewrites this to:
  `GET ${GATEWAY_BASE_URL}/movie-service/api/v1/movies?query=inception`

The reason for this proxy pattern is that Docker container hostnames don't resolve in a real user's browser — `gateway` only exists inside the Docker network. The frontend uses relative paths, Next.js handles the rewrite server-side, and the API client itself is environment-aware (different URL strategies for browser vs server-side rendering).

Inside the **gateway**, routes forward to the microservices:

- `/user-service/**` → `user-service`
- `/movie-service/**` → `movie-service`
- `/rating-service/**` → `rating-service`

### Aggregated catalog endpoint

The gateway also exposes an **aggregation endpoint** used by the movie-detail page:

- `GET /api/v1/catalog/movies/{id}`

It returns a merged view of multiple services:

```json
{
  "movie": { ... },              // from movie-service
  "ratingSummary": { ... },      // from rating-service
  "me": {
    "rating": 9.0,               // my rating (if logged in)
    "inWatchlist": true          // whether I've saved it
  }
}
```

Internally the gateway uses a load-balanced `WebClient` to call `movie-service` and `rating-service` in parallel via `Mono.zip`, forwarding the `Authorization` header so downstream services can resolve the current user. The aggregation lives at the gateway rather than inside any one service so the service boundaries stay clean — `movie-service` doesn't know about ratings, `rating-service` doesn't know about movies.

---

## Authentication

Authentication runs through stateless JWTs:

- `user-service` issues tokens on login, signs them with a shared secret, and embeds the user's email, roles, and ID in the claims.
- Each downstream service verifies the token locally using the same secret — no round-trip back to `user-service` to validate.
- Read endpoints (browsing the catalog, viewing aggregate ratings) are public; write endpoints require a valid token.

The local-verification approach was chosen over a shared auth module to keep services genuinely independent of each other. At three services, the duplication is trivially manageable; at larger scale it would make sense to extract a shared library.

---

## Observability & API documentation

Each **core microservice** (`user-service`, `movie-service`, `rating-service`) exposes:

### Health (Spring Boot Actuator)

- `GET /actuator/health` → returns `{ "status": "UP" }` when the service is healthy.

Locally (via exposed ports):

- User-service: `http://localhost:8082/actuator/health`
- Movie-service: `http://localhost:8083/actuator/health`
- Rating-service: `http://localhost:8084/actuator/health`

In production these endpoints sat inside the Docker network only and were intended for future load balancers / monitoring rather than public access.

### Metrics & dashboards (Micrometer + Prometheus + Grafana)

All four Spring Boot services (`gateway`, `user-service`, `movie-service`, `rating-service`) register a Micrometer Prometheus registry and expose:

- `GET /actuator/prometheus` → metrics scrape endpoint (HTTP server with latency histograms, JVM, HikariCP pool for the MVC services; route-level metrics on gateway)

The `prometheus` container scrapes all four services every 15s. The `grafana` container is preloaded with a Prometheus datasource and the **MicroFlix Overview** dashboard, both provisioned from `docker/grafana/provisioning/` and `docker/grafana/dashboards/`. The dashboard covers:

- Per-service request rate
- p50 / p95 / p99 HTTP request latency (computed from histogram buckets)
- HTTP status codes stacked by service and status
- JVM heap usage
- HikariCP connection pool (active + idle) for the three MVC services

Locally:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001` (admin/admin)

### OpenAPI / Swagger UI (springdoc-openapi)

- JSON docs: `GET /v3/api-docs`
- UI: `GET /swagger-ui/index.html`

Examples (local):

- `http://localhost:8082/swagger-ui/index.html` (user-service)
- `http://localhost:8083/swagger-ui/index.html` (movie-service)
- `http://localhost:8084/swagger-ui/index.html` (rating-service)

Swagger is mainly for development and debugging.

---

## Error handling

All services use Spring's `ProblemDetail` to implement **RFC 7807 problem+json** error responses:

- Domain errors (e.g., movie not found, rating not found) → **404** with a clear title.
- Bad input / validation errors → **400**, with a field error map when using `@Valid`.
- Generic unexpected errors → **500** with a safe, generic message.

The gateway's aggregation endpoint:

- Treats "no data yet" situations (e.g., no ratings, not in watchlist) as **empty defaults**.
- For real downstream errors, it **passes through** the microservice's `ProblemDetail` JSON (status + body) so the frontend sees a consistent error format regardless of whether it calls a single service or the aggregated catalog endpoint.

---

## Deployment (AWS EC2 + CI/CD)

MicroFlix was deployed to an **AWS EC2** instance running Docker and Docker Compose. The deployment is currently retired (see Project Status above), but everything below describes what was actually running.

The full stack ran on a `t3.medium` instance (`t3.micro` was attempted first but the combined memory footprint of multiple Spring Boot services plus three Postgres instances pushed it over). The stack was started with a production compose file:

- only the **frontend (port 80)** was exposed publicly
- microservices and databases stayed on the internal Docker network

### Running the TMDb ingestion job in production

On EC2, the ingestion job was run as a one-off container. From the `docker` directory on the EC2 instance:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --profile jobs run --rm tmdb-ingestion-service --count=50
```

This reused the existing production stack configuration, started the job container with access to `movie-service` and the movie database, inserted new TMDb movies, and exited. It could be triggered manually or wired into a cron job or scheduled GitHub Action.

> **Note:** After changing `tmdb-ingestion-service` code and deploying to EC2, rebuild the ingestion image before running the job:
>
> ```bash
> docker-compose -f docker-compose.yml -f docker-compose.prod.yml \
>   --profile jobs build tmdb-ingestion-service
> ```

### GitHub Actions

Two workflows kept the project building and deploying:

**CI** (`.github/workflows/ci.yml`)

- Ran on pushes and pull requests.
- Always:
  - Built and tested the backend (`mvn -B clean test` from the repo root).
  - Built the frontend (`npm ci && npm run build` in `frontend/`).
- On pushes to `main`:
  - Also built Docker images for all services (discovery, gateway, user-service, movie-service, rating-service, tmdb-ingestion, frontend).
  - Pushed them to Docker Hub under `kbrown2428/microflix-*` tagged `:latest`.

**Deploy to EC2** (`.github/workflows/cd.yml`)

- Triggered automatically via `workflow_run` **after the CI workflow completed successfully on `main`**, with manual `workflow_dispatch` as a backup.
- SSHed into the EC2 instance using GitHub secrets (`EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`).
- On the server:
  - Pruned old unused Docker artifacts.
  - Pulled the latest code (`git fetch` + `git reset --hard origin/main`).
  - Pulled the newest container images and restarted the stack:

    ```bash
    cd /opt/MicroFlix/docker
    IMAGE_TAG=latest docker-compose pull
    IMAGE_TAG=latest docker-compose up -d
    ```

EC2 itself never built images — it only pulled and ran the ones built by CI. The reason CD waits for CI rather than triggering directly off the same `push` event was a real production debugging story: the original setup had both workflows triggered by the same push, and the first deploy failed because EC2 tried to pull images that CI hadn't finished pushing yet. Treating "deploy from main" as one operation when it was actually two coordinated operations was the mistake. The `workflow_run` trigger codified the ordering that should always have been there.

Branch protection rules required the CI checks to pass before merging into `main`, and the deploy workflow only ran after a successful CI run.

---

## Where to look next

- `modules/user-service/README.md` – auth, profile, and error handling.
- `modules/movie-service/README.md` – search, genres, TMDb seeding, and indexing.
- `modules/tmdb-ingestion-service/README.md` – TMDb seeding + enrichment job and how to run it.
- `modules/rating-service/README.md` – ratings, watchlist, and engagement model.
- `modules/gateway/README.md` – routes and aggregated catalog endpoints.
- `frontend/README.md` – Next.js UI and how it talks to the gateway.
- `docs/explain-analyze.md` – measurement-driven index analysis: V4 baseline coverage, V5/V6 additions, and "considered but rejected" decisions.

---
