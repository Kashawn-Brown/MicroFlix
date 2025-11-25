
#  Movie Service


This is the **movie-service** for Microflix. It manages core movie metadata 
(create, list, and fetch movies) and can optionally seed realistic data from 
TMDb so the rest of the platform has a solid catalog to work with.

It follows the same patterns as `user-service`: Spring Boot 3, Java 21, Postgres, Flyway, 
and a thin controller + service + repository stack.

This setup runs the **movie-service** and **Postgres 18** database in Docker along with the 
rest of the Microflix stack, so you don’t need a local DB installed.

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

On startup, Flyway runs `V1__create_movies_table.sql` and creates a `movies` table with:

- `id BIGSERIAL PRIMARY KEY` – internal movie identifier used by the movie-service and other services
- `title VARCHAR(255) NOT NULL` – movie title
- `overview TEXT` – optional description / synopsis
- `release_year INT` – optional release year (e.g. 1999)
- `runtime INT` – optional runtime in minutes
- `tmdb_id BIGINT` – optional reference to the TMDb movie id for syncing/seeding
- `created_at TIMESTAMPTZ NOT NULL` – when the movie row was created
- `updated_at TIMESTAMPTZ NOT NULL` – when the movie row was last updated

The `tmdb_id` field lets the service link local movies to TMDb data. It is currently nullable so I can create local-only movies as well as TMDb-backed ones.

In later migrations I also added:

- A `genres` table – stores distinct genre names (e.g. "Action", "Comedy", "Sci-Fi").
- A `movie_genres` join table – links movies to genres using `(movie_id, genre_id)` with a uniqueness constraint so the same genre isn’t duplicated for a movie.
- Two extra columns on `movies`:
  - `poster_path` – path to the movie poster image from TMDb (when available).
  - `backdrop_path` – path to the backdrop image from TMDb (when available).

These are all internal to movie-service and are exposed to clients as simple string fields and a `List<String> genres` on the `MovieResponse` DTO.

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

# (or: docker compose up --build movie-db movie-service)
````

Once things are up, I expect to see logs for:

* `movie-db`:
  `database system is ready to accept connections`
* `movie-service`: Spring Boot startup banner + Flyway validation + `Started MovieServiceApplication...`
* \+ The rest of the Microflix stack starting up

At this point:

* `movie-service` is available directly at: `http://localhost:8083`
* Through the gateway, movie endpoints live under: `http://localhost:8081/api/v1/movies`

---

## API endpoints

I use these calls to confirm everything is working end-to-end via the gateway.

### **List / search movies (with filters and pagination)**

```bash
curl "http://localhost:8081/api/v1/movies?query=inception&genre=Action&year=2010&sort=created_desc&page=0&size=20"
````

The endpoint supports:

* `query` (optional) – case-insensitive substring match on `title`.
* `genre` (optional) – exact genre name (case-insensitive), e.g. `Action`, `Comedy`.
* `year` (optional) – exact `releaseYear`, e.g. `2010`.
* `sort` (optional) – simple sort key, e.g.:

  * `created_desc` (default)
  * `created_asc`
  * `title_asc` / `title_desc`
  * `year_asc` / `year_desc`
* `page` (optional) – zero-based page index, default `0`.
* `size` (optional) – page size, default `20`.

The response is a Spring `Page<MovieResponse>`, which serializes to JSON like:

```json
{
  "content": [
    {
      "id": 1,
      "title": "Inception",
      "overview": "A thief who steals corporate secrets...",
      "releaseYear": 2010,
      "runtime": 148,
      "tmdbId": 27205,
      "posterPath": "/qmDpIHrmpJINaRKAfWQfftjCdyi.jpg",
      "backdropPath": "/s3TBrRGB1iav7gFOCNx3H31MoES.jpg",
      "genres": ["Action", "Science Fiction"],
      "createdAt": "2025-11-17T20:26:35.490Z",
      "updatedAt": "2025-11-17T20:26:35.490Z"
    }
  ],
  "pageable": { "...": "..." },
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

If you call it with no query params:

```bash
curl http://localhost:8081/api/v1/movies
```

you get the first page of movies sorted by `createdAt` descending.


### **Get a specific movie by id**

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

### **Create a movie manually**

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
    * Poster and backdrop paths from TMDb are stored on the movie so the frontend can display images.
    * TMDb genre IDs are mapped to genre names and saved into the `genres` + `movie_genres` tables, so each seeded movie comes with a `genres` list.



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

