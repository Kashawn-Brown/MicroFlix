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

### Aggregated catalog endpoint

The gateway also exposes a **catalog aggregation** endpoint for the movie-detail page:

- `GET /api/v1/catalog/movies/{id}`

It returns a single JSON payload combining:

- Movie metadata from `movie-service`
- Rating summary (average + count) from `rating-service`
- The current user’s rating + watchlist flag from `rating-service`

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
````

Internally, a `CatalogService` uses a load-balanced `WebClient` to:

* Call `lb://movie-service/api/v1/movies/{id}`
* Call `lb://rating-service/api/v1/ratings/movie/{id}/summary`
* Call `lb://rating-service/api/v1/ratings/movie/{id}/me`
* Call `lb://rating-service/api/v1/engagements/watchlist/{id}/me`

If the incoming request has an `Authorization: Bearer <token>` header, it is **forwarded** to rating-service so it can resolve “me” using its existing auth logic.

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


