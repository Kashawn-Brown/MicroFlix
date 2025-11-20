
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

When I call them through the gateway, I use:

```text
http://localhost:8081/api/v1/ratings
```

### Create or upsert a rating

**POST** `/api/v1/ratings`

* Temporarily, I pass `userId` in the request body.
* Later, this will come from the authenticated user (JWT).

**Request body:**

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "movieId": 1,
  "rate": 8.7
}
```

Behavior:

* If a rating for `(userId, movieId)` does **not** exist, a new row is created.
* If it **does** exist, the existing rating is updated (upsert).

**Sample 201 response:**

```json
{
  "id": 1,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "movieId": 1,
  "rating": 8.7,
  "createdAt": "2025-11-19T04:20:00Z",
  "updatedAt": "2025-11-19T04:20:00Z"
}
```

---

### Update an existing rating

**PATCH** `/api/v1/ratings`

**Request body:**

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "movieId": 1,
  "rate": 9.2
}
```

If the rating exists for `(userId, movieId)`, it updates the stored score.
If it does **not** exist, the service throws `RatingNotFoundException` and returns a `404` ProblemDetail response (see Errors below).

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

## Future work

Later improvements I plan to make:

* Integrate **JWT auth** so `userId` comes from the authenticated user instead of the request body.
* Add endpoints for aggregate stats (e.g. average rating per movie).
* Potentially share a common `CurrentUser` pattern across services, similar to user-service.

---