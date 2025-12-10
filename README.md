# Microflix

Microflix is a microservices-based movie platform I’m building to practice production-minded backend development.

Right now the focus is on a solid `user-service` with proper auth, profile management, tests, and a local Docker stack that feels close to how a real system would run.

---

## Stack overview

Current modules:

- `modules/user-service` – user registration, login (JWT), and profile (`/users/me`, update profile, change password).
- `modules/movie-service` – movie metadata service with basic CRUD, search/filter/sort + pagination, and optional TMDb-based seeding.
- `modules/rating-service` – movie ratings service (1–10 scale with 0.1 increments; per-user per-movie ratings) + simple watchlist feature via an engagement table.
- `modules/gateway` – Spring Cloud Gateway entrypoint for the API.
- `modules/discovery` – Eureka discovery server.
- `docker/` – Docker Compose setup for running the local stack.
- `frontend/` – Next.js app that talks to the gateway and provides a simple UI for auth, browsing movies, rating, and watchlist management. :contentReference[oaicite:0]{index=0}

Key technologies:

- Java 21, Spring Boot 3
- Spring Security with JWT (HS256)
- Spring Cloud Gateway + Eureka discovery
- Postgres 18 + Flyway
- Docker + Docker Compose
- JUnit 5 + Mockito for tests

---

## Backend

I use Docker Compose to run the core services together:

- `discovery` (Eureka server) on port **8761**
- `gateway` (API entrypoint) on port **8081**
- `user-service` on port **8082** (behind gateway at `/api/v1/users`)
- `user-db` (Postgres 18) with a named volume for data
- `movie-service` on port **8083** (behind gateway at `/api/v1/movies`)
- `movie-db` (Postgres 18) with its own named volume
- `rating-service` on port **8084** (behind gateway at `/api/v1/ratings`)
- `rating-db` (Postgres 18) with its own named volume


From the `docker/` directory:

```bash
cd docker
docker compose up --build
```

## Frontend (Next.js)

The `frontend/` folder contains a small Next.js app that sits in front of the gateway:

- **Stack:** Next.js (App Router) + TypeScript + React + Tailwind CSS.
- **Backend integration:** All API calls go through the gateway on `http://localhost:8081` using typed helpers (auth, movies, ratings, watchlist).
- **Auth:** Users can register/login against `user-service`. A JWT is stored in `localStorage` and sent as `Authorization: Bearer <token>` to protected endpoints. Expired tokens trigger a quiet “auto-logout” and redirect back to `/login`.
- **Core screens:**
    - Home page with a short intro, feature CTA, and a \"recently added\" rail.
    - Movies list (`/movies`) with search, genre filter, year filter, sort options, pagination, and a page-size selector.
    - Movie details (`/movies/[id]`) with poster/backdrop, genres, overview, rating summary, your rating (create/update/delete), and a watchlist toggle.
    - Watchlist page (`/watchlist`) showing the user’s saved movies, with remove + link to details.
    - Simple profile area with a \"My ratings\" page that lists all movies the user has rated.

To run the frontend:

```bash
cd frontend
npm install
npm run dev
```

## Observability & API Documentation

Each core microservice (user-service, movie-service, rating-service) exposes:

- **Health endpoint** via Spring Boot Actuator:

  - `GET /actuator/health` → returns `{ "status": "UP" }` when the service is healthy.
  - Used for Docker / future load balancers / uptime checks.
  - In production, these endpoints are only reachable on the internal Docker network (EC2), not exposed publicly.

- **OpenAPI / Swagger UI** via springdoc-openapi:

  - `GET /v3/api-docs` → raw OpenAPI JSON
  - `GET /swagger-ui/index.html` → interactive Swagger UI

Swagger is primarily used during development and debugging to inspect and test endpoints.  
Actuator health endpoints are the starting point for future monitoring and alerts (e.g., CloudWatch, Prometheus).
