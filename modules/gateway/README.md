# Gateway Service

The `gateway` is the **single entrypoint** to the MicroFlix backend.

It is built with **Spring Cloud Gateway** (WebFlux) and **Eureka Discovery** and is responsible for:

- Routing HTTP requests to the appropriate microservice
- Providing a stable URL surface for the frontend
- Exposing **aggregated catalog endpoints** that combine data from multiple services

---

## Responsibilities

### Routing

The gateway uses service discovery names (via Eureka) to forward traffic:

- `/user-service/**` → `user-service`
- `/movie-service/**` → `movie-service`
- `/rating-service/**` → `rating-service`

In production, the **Next.js frontend** talks only to the gateway. It uses a `/gateway/...` prefix that Next.js rewrites to these internal gateway paths.

Example from the browser:

- Browser: `GET /gateway/movie-service/api/v1/movies?query=inception`
- Next.js rewrite: `GET ${GATEWAY_BASE_URL}/movie-service/api/v1/movies?query=inception`

### Aggregated catalog endpoints

The gateway exposes two **catalog aggregation** endpoints that the frontend hits directly. Both live in `CatalogController` and are backed by `CatalogService` using a load-balanced `WebClient` over Eureka (`lb://...`). The `Authorization` header is **forwarded** to downstream services so they can resolve the current user using their existing auth logic.

#### Movie-detail aggregation — `GET /api/v1/catalog/movies/{id}`

Returns a single JSON payload combining:

- Movie metadata from `movie-service`
- Rating summary (average + count) from `rating-service`
- The current user's rating + watchlist flag from `rating-service`

Example shape:

```json
{
  "movie": {
    "id": 123,
    "title": "Inception",
    "releaseYear": 2010,
    "overview": "...",
    "genres": ["Action", "Sci-Fi"],
    "posterPath": "/some/poster.jpg",
    "backdropPath": "/some/backdrop.jpg"
  },
  "ratingSummary": {
    "average": 8.4,
    "count": 123
  },
  "me": {
    "rating": 9.0,
    "inWatchlist": true
  }
}
```

Fans out in parallel via `Mono.zip`:

* `lb://movie-service/api/v1/movies/{id}`
* `lb://rating-service/api/v1/ratings/movie/{id}/summary`
* `lb://rating-service/api/v1/ratings/movie/{id}/me`
* `lb://rating-service/api/v1/engagements/watchlist/{id}/me`

When no `Authorization` header is present, the `me` block is short-circuited server-side to `{ "rating": null, "inWatchlist": false }` without the two authed calls.

#### Watchlist aggregation — `GET /api/v1/catalog/watchlist` (authed only)

Returns the current user's watchlist already joined with movie metadata, sorted `addedAt` desc:

```json
[
  {
    "movie": {
      "id": 123,
      "title": "Inception",
      "releaseYear": 2010,
      "genres": ["Action", "Sci-Fi"],
      "posterPath": "/some/poster.jpg",
      "backdropPath": "/some/backdrop.jpg"
    },
    "addedAt": "2026-04-22T10:15:30Z"
  }
]
```

Fans out sequentially — it has to, since the second call depends on the first:

1. `lb://rating-service/api/v1/engagements/watchlist` → list of engagement rows (`userId`, `movieId`, `addedAt`)
2. `lb://movie-service/api/v1/movies/batch?ids=<comma-sep>` → hydrated movie metadata in one round-trip

`CatalogService.joinWatchlist` zips the two by `movieId`, preserves the engagements' `addedAt` desc order, and **silently drops any engagement whose `movieId` no longer exists in movie-service** (stale engagement to a deleted movie). This replaces the pre-migration 1 + N browser-side fan-out with a single client request.

---

## Error handling

For the aggregation endpoints:

* “Optional” data (no ratings yet, not in watchlist) is treated as **empty defaults**:

    * summary defaults to `{ "average": null, "count": 0 }`
    * `me.rating` may be `null`
    * `me.inWatchlist` defaults to `false`

* For real downstream errors (e.g., movie not found, auth failures), the gateway’s global `GatewayExceptionHandler`:

    * catches `WebClientResponseException`,
    * **passes through** the downstream status and `ProblemDetail` JSON body unchanged.

This keeps error handling consistent: the frontend sees the same `ProblemDetail` shape whether it calls a microservice route or the aggregated `/api/v1/catalog/movies/{id}` endpoint.

---

## Running locally

The gateway runs as part of the Docker Compose stack:

```bash
cd docker
docker compose up --build
```

* Gateway: `http://localhost:8081`
* It expects `discovery` (Eureka) and the other services to be up as defined in `docker-compose.yml`.

To run only the gateway:

```bash
cd modules/gateway
mvn spring-boot:run
```

Configure Eureka client and routes in `application.yml` as usual.

---

## Observability

The gateway registers a Micrometer Prometheus registry and exposes Actuator on port 8081:

- `GET /actuator/health` → `{ "status": "UP" }` when up
- `GET /actuator/prometheus` → metrics scrape endpoint
- `GET /actuator/gateway/routes` → introspect the active routes (read-only)

Spring Cloud Gateway 2025.0.0 auto-emits route-level metrics with no extra config. HTTP request histogram buckets are enabled (`management.metrics.distribution.percentiles-histogram.http.server.requests: true`) so the **MicroFlix Overview** Grafana dashboard can compute p50 / p95 / p99 latency for the gateway alongside the downstream services.

The `prometheus` container scrapes `gateway:8081/actuator/prometheus` every 15s.

---

## Load testing

Both aggregation endpoints are the target of the Branch 3 frontend migration. Baseline (pre-migration, individual service endpoints from the frontend) and aggregated (post-migration, single catalog call) measurements are captured as median-of-3 runs in [`docs/benchmarks.md`](../../docs/benchmarks.md); load scripts in `k6/scenarios/` (`watchlist-{baseline,aggregated}.js`, `movie-detail-{baseline,aggregated}.js`).

---


