
# Rating Service

This is the **rating-service** for Microflix. It lets users rate movies on a 1–10 scale with 0.1 increments (e.g. `8.1/10`) and exposes endpoints to create/update ratings and query them by movie or user.

It follows the same patterns as `user-service` and `movie-service`: Spring Boot 3, Java 21, Postgres, Flyway, and a thin controller + service + repository stack.

This setup runs the **rating-service** and **Postgres 18** database in Docker along with the rest of the Microflix stack, so you don’t need a local DB installed.

### Prerequisites

- Docker / Docker Desktop
- Java 21 + Maven (only needed if I run the app locally outside Docker)


### What’s running

From `docker/docker-compose.yml`, I have two main services:

* **rating-db**

  * Image: `postgres:18`
  * Host port: `5436` → container port `5432`
  * Database: `ratingdb`
  * Main table: `ratings`
  * Username/password: `rating` / `rating`
  * Data is stored in a named volume `pg_rating_data`, so it survives container restarts.
  * Schema created by Flyway migration `V1__init_ratings.sql`

* **rating-service**

  * Built from `modules/rating-service/Dockerfile` (multi-stage: Maven build + slim Java 21 runtime).
  * Exposed on `http://localhost:8084`
  * Connects to Postgres using `jdbc:postgresql://rating-db:5432/ratingdb`.
  * Registers with Eureka so the **gateway** can route `/api/v1/ratings/**` calls to it.

Other services (for context):

- `discovery` on `http://localhost:8761` (Eureka dashboard)
- `gateway` on `http://localhost:8081` (API entrypoint)

---

## Tech stack

- Java 21
- Spring Boot 3.x
- Spring Data JPA (Postgres)
- Flyway for database migrations
- JUnit 5 + Mockito for tests
- Docker + Docker Compose for local infra

---

## Database schema

On startup, Flyway runs `V1__create_ratings_table.sql` and creates a `ratings` table with:

- `id BIGSERIAL` – primary key
- `user_id UUID NOT NULL` – logical FK to `user-service` users
- `movie_id BIGINT NOT NULL` – logical FK to `movie-service` movies
- `rating_times_ten INT NOT NULL` – rating stored as 10–100 representing 1.0–10.0 in 0.1 increments  
  (e.g. 81 = 8.1/10)
- `created_at TIMESTAMPTZ NOT NULL`
- `updated_at TIMESTAMPTZ NOT NULL`

There is a unique constraint on `(user_id, movie_id)`, so each user can only have **one rating per movie**.



A later migration adds an `engagements` table to support watchlist-style features:

- `id BIGSERIAL PRIMARY KEY`
- `user_id UUID NOT NULL` – logical FK to `user-service` users
- `movie_id BIGINT NOT NULL` – logical FK to `movie-service` movies
- `type VARCHAR(32) NOT NULL` – engagement type enum (e.g. `WATCHLIST`, `FAVOURITE`, `LIKE`)
- `created_at TIMESTAMPTZ NOT NULL`

There is a unique constraint on `(user_id, movie_id, type)`, so a user can only have one engagement of a given type for a movie (e.g. one `WATCHLIST` row per `(user, movie)`).

---

## How I start the stack 

###### Running locally with Docker Compose:

From the repo root there is a `docker/` directory that orchestrates all services.

From there:

```bash
# Stop any old containers for this compose file
docker compose down

# Build images and start the full stack (discovery, gateway, services, and DBs)
docker compose up --build

# (or: docker compose up --build rating-db rating-service)
```

Once things are up, I expect to see logs for:

* `pg-rating` (Postgres): `database system is ready to accept connections`
* `rating-service`: the Spring Boot startup banner + Flyway migrations + “`Started RatingServiceApplication...`”
* \+ The rest of the Microflix stack starting up

At this point:

* `rating-service` is available directly at: `http://localhost:8084`
* Through the gateway, rating endpoints live under: `http://localhost:8081/api/v1/ratings`

---

## API overview

All endpoints are rooted at:

```text
/api/v1/ratings
```
For write operations, `userId` comes from the authenticated user (JWT) rather than the request body. Read operations are public.

When I call them through the gateway, I use:

```text
http://localhost:8081/api/v1/ratings
```

### Create or upsert a rating (current user)

**POST** `/api/v1/ratings`

Requires a valid JWT from user-service. The `userId` is taken from the token; the body only needs movie + score.

**Request body:**

```json
{
  "movieId": 1,
  "rate": 8.7
}
````

Behavior:

* If a rating for `(currentUserId, movieId)` does **not** exist, a new row is created.
* If it **does** exist, the existing rating is updated (upsert).

**Sample 201 response:**

```json
{
  "id": 1,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "movieId": 1,
  "rate": 8.7,
  "createdAt": "2025-11-19T04:20:00Z",
  "updatedAt": "2025-11-19T04:20:00Z"
}
```


---

### Update an existing rating (current user)

**PATCH** `/api/v1/ratings`

Requires JWT. Uses the current user from the token and the `movieId` + new `rate` from the body.

**Request body:**

```json
{
  "movieId": 1,
  "rate": 9.2
}
````

If the rating exists for `(currentUserId, movieId)`, it updates the stored score.

If it does **not** exist, the service throws `RatingNotFoundException` and returns a `404` ProblemDetail response.

---

### Get all ratings for a movie

**GET** `/api/v1/ratings/movie/{movieId}`

Example:

```http
GET http://localhost:8081/api/v1/ratings/movie/1
```

Sample response:

```json
[
  {
    "id": 1,
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "movieId": 1,
    "rating": 8.7,
    "createdAt": "2025-11-19T04:20:00Z",
    "updatedAt": "2025-11-19T04:20:00Z"
  },
  {
    "id": 2,
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "movieId": 1,
    "rating": 6.5,
    "createdAt": "2025-11-19T04:22:00Z",
    "updatedAt": "2025-11-19T04:22:00Z"
  }
]
```

---

### Get all ratings for the current user (via JWT)

**GET** `/api/v1/ratings/user`

Requires JWT. Uses the current user from the token.


```http
GET http://localhost:8081/api/v1/ratings/user
```

Returns a list of `RatingResponse` for the authenticated user across all movies.

---

### Get all ratings for a user

**GET** `/api/v1/ratings/user/{userId}`

```http
GET http://localhost:8081/api/v1/ratings/user/550e8400-e29b-41d4-a716-446655440000
```

Returns a list of `RatingResponse` for that user across all movies.

---

### Get a specific user’s rating for a movie

**GET** `/api/v1/ratings/movie/{movieId}/user/{userId}`

```http
GET http://localhost:8081/api/v1/ratings/movie/1/user/550e8400-e29b-41d4-a716-446655440000
```

Returns a single `RatingResponse` if found.
If nothing exists for that pair, the service returns a `404` ProblemDetail.

---

### Get a rating by ID

**GET** `/api/v1/ratings/{ratingId}`

```http
GET http://localhost:8081/api/v1/ratings/1
```

Returns the rating with the given `id`, or a `404` ProblemDetail if it doesn’t exist.

---

### Delete the current user's rating for a movie

**DELETE** `/api/v1/ratings/movie/{movieId}`

```http
DELETE http://localhost:8081/api/v1/ratings/movie/1
Authorization: Bearer <JWT>
````

Deletes the rating for `(currentUserId, movieId)` if it exists. This is idempotent from the client’s perspective:

* If a rating existed and was deleted → `204 No Content`.
* If no rating existed for that pair → still `204 No Content`.

---

## Watchlist / engagements API

The rating-service also exposes a simple watchlist feature using an `engagements` table. For now, only the `WATCHLIST` engagement type is used.

All of these endpoints require a valid JWT; the `userId` comes from the token.

Base path (through the gateway):

```text
http://localhost:8081/api/v1/engagements
````

### Add a movie to the current user's watchlist

**PUT** `/api/v1/engagements/watchlist/{movieId}`

```http
PUT http://localhost:8081/api/v1/engagements/watchlist/1
Authorization: Bearer <JWT>
```

Behavior:

* If the movie is not yet on the user's watchlist, a new `WATCHLIST` engagement row is created.
* If it is already on the watchlist, the call is a no-op.

Response:

* `204 No Content` on success.

### Remove a movie from the current user's watchlist

**DELETE** `/api/v1/engagements/watchlist/{movieId}`

```http
DELETE http://localhost:8081/api/v1/engagements/watchlist/1
Authorization: Bearer <JWT>
```

Behavior:

* Deletes the `WATCHLIST` engagement for `(currentUserId, movieId)` if it exists.
* Idempotent: if it wasn't there, you still get `204 No Content`.

### Get the current user's watchlist

**GET** `/api/v1/engagements/watchlist`

```http
GET http://localhost:8081/api/v1/engagements/watchlist
Authorization: Bearer <JWT>
```

Returns a JSON array of engagements for the current user. Example:

```json
[
  {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "movieId": 1,
    "type": "WATCHLIST",
    "addedAt": "2025-11-21T12:00:00Z"
  },
  {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "movieId": 42,
    "type": "WATCHLIST",
    "addedAt": "2025-11-20T09:30:00Z"
  }
]
```


---

## Error handling

The rating-service uses a `RatingErrorAdvice` with `ProblemDetail` responses:

* When a rating is not found (e.g. invalid `ratingId`, or missing `(userId, movieId)` pair):

```json
{
  "type": "about:blank",
  "title": "Rating not found",
  "status": 404,
  "detail": "Rating 99 was not found"
}
```

* When the rating value is invalid (e.g. outside 1.0–10.0):

```json
{
  "type": "about:blank",
  "title": "Invalid rating request",
  "status": 400,
  "detail": "Rating must be between 1.0 and 10.0"
}
```

This keeps the controllers clean and centralizes HTTP error representation, similar to `movie-service`.

---

## Sanity checks I usually run

After `docker compose up --build` from the `docker/` folder, I sanity-check rating-service via the gateway.

1. **Create a rating**

```http
POST http://localhost:8081/api/v1/ratings
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "movieId": 1,
  "rate": 8.7
}
```

I expect a `201 Created` with a JSON body containing `rating: 8.7`.

2. **Update that rating**

```http
PATCH http://localhost:8081/api/v1/ratings
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "movieId": 1,
  "rate": 9.0
}
```

I expect a `200 OK` and `rating: 9.0` in the response.

3. **List ratings for the movie**

```http
GET http://localhost:8081/api/v1/ratings/movie/1
```

I expect a non-empty JSON array including my updated rating.

4. **Get rating summary for a movie**

```http
GET http://localhost:8081/api/v1/ratings/movie/1/summary
```

I expect a `200 OK` with a JSON body like:

```json
{
  "movieId": 1,
  "average": 8.1,
  "count": 5
}
```

If there are no ratings yet for that movie, I still expect `200 OK`, but with:

```json
{
  "movieId": 1,
  "average": null,
  "count": 0
}
```

---

## Running tests

From the repo root, I can run just rating-service tests with:

```bash
mvn test -pl modules/rating-service
```

This runs:

* `RatingServiceTest` – covers upsert logic, score conversion, and not-found cases.
* `RatingControllerTest` – verifies HTTP status codes and that the controller delegates to the service correctly.

If everything is green, I know the core behavior of rating-service is stable.

---

## Authentication & error shape

For rating writes and watchlist operations, I rely on the JWT issued by the user-service:

- `POST /api/v1/ratings`, `PATCH /api/v1/ratings`, `DELETE /api/v1/ratings/movie/{movieId}` all require a valid JWT.
- Watchlist endpoints under `/api/v1/engagements/watchlist/**` also require JWT.
- Read-only endpoints (`GET /api/v1/ratings/**` and `GET /api/v1/engagements/watchlist`) are public only in the sense that they don't require extra roles; they still use the current user for `/user` and watchlist calls.

The `userId` always comes from the authenticated principal (`CurrentUser`) rather than from the request body.

I send the token as:

```http
Authorization: Bearer <JWT>
````

If I try to create/update a rating without a valid token (missing, invalid, or expired), the `JwtAuthenticationEntryPoint` returns a `401 Unauthorized` in `ProblemDetail` format:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required to create or update ratings.",
  "path": "/api/v1/ratings",
  "timestamp": "2025-11-20T12:34:56.789Z"
}
```

Other rating errors (like “rating not found” or invalid score) are handled by `RatingErrorAdvice` and also returned as `ProblemDetail`.

---

## Observability

The rating-service exposes basic health information via Spring Boot Actuator.

- **Health check**

  - `GET /actuator/health`
  - Returns `{ "status": "UP" }` when the service is healthy.

> In production (AWS), this endpoint is only reachable from inside the Docker network:
>
> ```bash
> curl http://rating-service:8084/actuator/health
> ```

## API Documentation (OpenAPI / Swagger)

- **OpenAPI JSON**

  - `GET /v3/api-docs`

- **Swagger UI**

  - `GET /swagger-ui/index.html`

The docs cover rating + watchlist endpoints such as:

- `PUT    /api/v1/ratings/movie/{movieId}` (rate a movie)
- `GET    /api/v1/ratings/movie/{movieId}/summary`
- `GET    /api/v1/ratings/me`
- `DELETE /api/v1/ratings/movie/{movieId}`
- `POST   /api/v1/engagements/watchlist/{movieId}`
- `DELETE /api/v1/engagements/watchlist/{movieId}`
- `GET    /api/v1/engagements/watchlist/me`

Swagger UI is meant for internal debugging and contract inspection.


---

## Future work

Later improvements I plan to make:


---

