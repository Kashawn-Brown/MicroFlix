
---

# 🐳 Local Dev: Running `movie-service` + Postgres with Docker Compose

This setup runs the **movie-service** and its **Postgres 18** database in Docker, along with the rest of the Microflix stack (discovery + gateway + user-service). I don’t need a local Postgres installed.

### Prerequisites

- Docker / Docker Desktop
- Java 21 + Maven (only needed if I run the app locally outside Docker)

### What’s running

From `docker/docker-compose.yml`, the pieces relevant to movies are:

- **movie-db**

  - Image: `postgres:18`
  - Host port: `5435` → container port `5432`
  - Database: `moviedb`
  - Main table: `movies`
  - Username/password: `movie` / `movie`
  - Data is stored in a named volume (e.g. `pg_movie_data`), so it survives container restarts.
  - Schema created by Flyway migration `V1__create_movies_table.sql`

- **movie-service**

  - Built from `modules/movie-service/Dockerfile` (multi-stage: Maven build + slim Java 21 runtime).
  - Exposed on `http://localhost:8083`
  - Connects to Postgres using `jdbc:postgresql://movie-db:5432/moviedb`.
  - Registers with Eureka so the **gateway** can route `/api/v1/movies/**` calls to it.
  - Can optionally seed data from TMDb when I enable it with env vars (see below).

Other services (for context):

- `discovery` on `http://localhost:8761` (Eureka dashboard)
- `gateway` on `http://localhost:8081` (API entrypoint)
- `user-service` + `user-db` (auth/profile)

### How I start the stack

From the `docker/` directory:

```bash
# Stop any old containers for this compose file
docker compose down

# Build images and start the full stack (discovery, gateway, user-service, movie-service, and DBs)
docker compose up --build
````

Once things are up, I expect to see logs for:

* `movie-db`:
  `database system is ready to accept connections`
* `movie-service`: Spring Boot startup banner, Flyway validation, and
  `Started MovieServiceApplication...`

At this point:

* `movie-service` is available directly at: `http://localhost:8083`
* Through the gateway, movie endpoints live under: `http://localhost:8081/api/v1/movies`

---

## API endpoints

I use these calls to confirm everything is working end-to-end via the gateway.

### **1️⃣ List all movies**

```bash
curl http://localhost:8081/api/v1/movies
```

If I’ve seeded data (see TMDb seeding below) or created some movies manually, I expect a `200 OK` with a JSON array of `MovieResponse` objects:

```json
[
  {
    "id": 1,
    "title": "Inception",
    "overview": "A thief who steals corporate secrets...",
    "releaseYear": 2010,
    "runtime": 148,
    "tmdbId": 27205,
    "createdAt": "2025-11-17T20:26:35.490Z",
    "updatedAt": "2025-11-17T20:26:35.490Z"
  }
]
```

### **2️⃣ Get a specific movie by id**

```bash
curl http://localhost:8081/api/v1/movies/1
```

* If the movie exists → I get `200 OK` with a single `MovieResponse`.
* If it does **not** exist → `MovieNotFoundException` is thrown and mapped to a `404 Not Found` with a `ProblemDetail` JSON body:

```json
{
  "type": "about:blank",
  "title": "Movie not found",
  "status": 404,
  "detail": "No movie with id: 999999 found"
}
```

### **3️⃣ Create a movie manually**

```bash
curl -X POST http://localhost:8081/api/v1/movies \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tenet",
    "overview": "Time inversion thriller.",
    "releaseYear": 2020,
    "runtime": 150,
    "tmdbId": 577922
  }'
```

I expect:

* HTTP `201 Created`
* Response body with the generated `id`, timestamps, and the fields I sent.

---

## TMDb seeding 

The movie-service can seed the local database with movies from TMDb. It uses their 'Now Playing', 'Popular', 'Top Rated', and 'Upcoming' movie lists to seed the db

This is **opt-in** and controlled by configuration so it doesn’t surprise me in every environment.



**How it works:**

* On startup, a `CommandLineRunner` (`MovieSeeder`) runs **only if** `movie.tmdb.seed.enabled=true`.
* `MovieSeeder` uses `TmdbClient` to call the TMDb  endpoints.
* For each returned movie:
    * If `tmdbId` is `null` → it is skipped.
    * If a movie with that `tmdbId` already exists → it is skipped (idempotent).
    * Otherwise a new row is created in `movies`


### **Configuration**

In `application.yml`:
```yaml
# application.yml (movie-service)
tmdb:
  base-url: https://api.themoviedb.org/3

movie:
  tmdb:
    seed:
      enabled: false   # default: seeding is OFF
```

Environment variables expected:

* `TMDB_API_KEY` → mapped to `tmdb.api-key`
* `MOVIE_TMDB_SEED_ENABLED` → mapped to `movie.tmdb.seed.enabled`


### **Using `.env` with Docker Compose:**

In `docker/.env` (not committed to git):

```env
TMDB_API_KEY=tmdb_api_key_here
MOVIE_TMDB_SEED_ENABLED=true
```

In `docker/docker-compose.yml` for `movie-service`:

```yaml
movie-service:
  environment:
    - ...
    - TMDB_API_KEY=${TMDB_API_KEY}
    - MOVIE_TMDB_SEED_ENABLED=${MOVIE_TMDB_SEED_ENABLED}
```

Spring Boot maps:

* `TMDB_API_KEY` → `tmdb.api-key`
* `MOVIE_TMDB_SEED_ENABLED` → `movie.tmdb.seed.enabled`

When I bring the stack up with `docker compose up --build`, I should see logs like:

```text
MovieSeeder started (movie.tmdb.seed.enabled=true)
Fetching popular movies from TMDb
MovieSeeder finished. Inserted X movies, skipped Y
```

After that, `GET /api/v1/movies` should return those seeded movies.

### Stopping and resetting the environment

To stop the containers (but keep DB data):

```bash
docker compose down
```

To completely reset the DB (drop all data and rerun migrations):

```bash
docker compose down -v   # removes containers + volumes
docker compose up --build
```

That gives me a clean `moviedb` with Flyway recreating the `movies` table from scratch.

---

## Testing

I keep the `movie-service` covered by service-level and controller-level tests so I can refactor without guessing.

### Prerequisites

* Java 21 installed and on my `PATH`.
* Maven wrapper (`./mvnw`) at the project root.
* Docker / Postgres are **not** required for these tests; they use mocks and run entirely in memory.

### Running movie-service tests

From the project root:

```bash
./mvnw test -pl movie-service
```

This tells Maven to only build and test the `movie-service` module.
I should see `BUILD SUCCESS` if everything passes.

Alternatively, from inside the module:

```bash
cd modules/movie-service
mvn test
```

### What the tests cover right now

Current tests focus on the core movie flows:

* **`MovieServiceTest`**

    * `getAllMovies` maps `Movie` entities to `MovieResponse` correctly.
    * `getMovie` returns the expected `MovieResponse` when the movie exists.
    * `getMovie` throws `MovieNotFoundException` when the movie does not exist.
    * `createMovie` builds a `Movie` from `CreateMovieRequest`, saves it, and returns the mapped response.

* **`MovieControllerTest`**

    * `GET /api/v1/movies` returns `200 OK` with a list of movies and delegates to `MovieService`.
    * `GET /api/v1/movies/{id}` returns `200 OK` for existing IDs and calls `movieService.getMovie(id)`.
    * `POST /api/v1/movies` returns `201 Created` and calls `movieService.createMovie(request)`.

As I expand movie-service (search, filtering, syncing more fields from TMDb), I’ll add or update tests alongside those changes.

### Troubleshooting

* If tests fail with missing classes or beans, I make sure I’m running from the project root with `./mvnw test -pl movie-service`.
* If a test fails unexpectedly, I check the stack trace to see whether it came from the service or controller layer and adjust either the test or implementation to match the intended behavior.

---

## Error handling

* Missing movies:

    * `MovieService.getMovie` throws `MovieNotFoundException` when a movie id is not found.
    * `MovieExceptionHandler` maps that to a `404 Not Found` with a `ProblemDetail` JSON payload.

This keeps the controller code simple and gives clients a clear, consistent error format.

---

## Future plans

Later, extend movie-service to:

* Fetch full movie details (including runtime and genres) from TMDb via `/movie/{id}`.
* Support search and filtering endpoints for the frontend.
* Add background jobs to periodically sync or enrich movies in the local database.

---

