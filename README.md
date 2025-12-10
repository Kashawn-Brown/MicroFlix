# MicroFlix

MicroFlix is a microservices-based movie platform I’m building to practice **production-minded backend development** and full-stack integration.

The system is split into small Spring Boot services (users, movies, ratings) behind a Spring Cloud Gateway, with a Next.js frontend on top and a Docker / AWS EC2 deployment that feels close to a real-world setup. :contentReference[oaicite:0]{index=0}

---

## Architecture overview

**Modules:**

- `modules/user-service`  
  User registration, login (JWT), `/users/me`, profile update, and password change.

- `modules/movie-service`  
  Movie metadata (title, overview, year, genres, poster/backdrop) with search/filter/sort + pagination and TMDb-based seeding.

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

- Java 21, Spring Boot 3
- Spring Web, Spring Data JPA, Spring Security (JWT)
- Spring Cloud Gateway + Eureka Discovery
- PostgreSQL + Flyway migrations
- Next.js (App Router), TypeScript, React 18, Tailwind CSS
- Docker + Docker Compose
- JUnit 5 + Mockito
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
````

This brings up:

* `discovery` (Eureka) on **[http://localhost:8761](http://localhost:8761)**
* `gateway` on **[http://localhost:8081](http://localhost:8081)**
* `user-service` on **[http://localhost:8082](http://localhost:8082)**
* `movie-service` on **[http://localhost:8083](http://localhost:8083)**
* `rating-service` on **[http://localhost:8084](http://localhost:8084)**
* three Postgres instances (`user-db`, `movie-db`, `rating-db`)
* `frontend` on **[http://localhost:80](http://localhost:80)** (or `http://localhost:3000` in dev-only setups)

The frontend talks only to the **gateway**, not directly to the microservices.

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

From the **frontend’s perspective**, all API calls go through a `/gateway` prefix that Next.js rewrites to the gateway. For example:

* Browser calls:
  `GET /gateway/movie-service/api/v1/movies?query=inception`
* Next.js rewrites this to:
  `GET ${GATEWAY_BASE_URL}/movie-service/api/v1/movies?query=inception`

Inside the **gateway**, routes forward to the microservices:

* `/user-service/**` → `user-service`
* `/movie-service/**` → `movie-service`
* `/rating-service/**` → `rating-service`

### Aggregated catalog endpoint

The gateway also exposes an **aggregation endpoint** used by the movie-detail page:

* `GET /api/v1/catalog/movies/{id}`

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

Internally the gateway uses a load-balanced `WebClient` to call `movie-service` and `rating-service`, forwarding the `Authorization` header so downstream services can resolve the current user.

---

## Observability & API documentation

Each **core microservice** (`user-service`, `movie-service`, `rating-service`) exposes:

### Health (Spring Boot Actuator)

* `GET /actuator/health` → returns `{ "status": "UP" }` when the service is healthy.

Locally (via exposed ports):

* User-service: `http://localhost:8082/actuator/health`
* Movie-service: `http://localhost:8083/actuator/health`
* Rating-service: `http://localhost:8084/actuator/health`

In production (AWS EC2), these endpoints are reachable **inside the Docker network** only and are meant for future load balancers / monitoring, not public access.

### OpenAPI / Swagger UI (springdoc-openapi)

* JSON docs: `GET /v3/api-docs`
* UI: `GET /swagger-ui/index.html`

Examples (local):

* `http://localhost:8082/swagger-ui/index.html` (user-service)
* `http://localhost:8083/swagger-ui/index.html` (movie-service)
* `http://localhost:8084/swagger-ui/index.html` (rating-service)

Swagger is mainly for development and debugging; in production it remains accessible on the internal network.

---

## Error handling

All services use Spring’s `ProblemDetail` to implement **RFC 7807 problem+json** error responses:

* Domain errors (e.g., movie not found, rating not found) → **404** with a clear title.
* Bad input / validation errors → **400**, with a field error map when using `@Valid`.
* Generic unexpected errors → **500** with a safe, generic message.

The gateway’s aggregation endpoint:

* Treats “no data yet” situations (e.g., no ratings, not in watchlist) as **empty defaults**.
* For real downstream errors, it **passes through** the microservice’s `ProblemDetail` JSON (status + body) so the frontend sees a consistent error format regardless of whether it calls a single service or the aggregated catalog endpoint.

---

## Deployment (AWS EC2 + CI/CD)

MicroFlix is deployed to an **AWS EC2** instance running Docker and Docker Compose:

* An EC2 `t3.medium` hosts the full stack (discovery, gateway, services, Postgres, frontend).
* The stack is started with a production compose file:

  * only the **frontend (port 80)** is exposed publicly;
  * microservices and databases stay on the internal Docker network.

### GitHub Actions

Two main workflows:

* **CI** (`.github/workflows/ci.yml`)

  * Runs on push/PR to `main`.
  * Builds and tests backend (`mvn clean test`) and frontend (`npm ci && npm run build`).

* **Deploy to EC2** (`.github/workflows/deploy-ec2.yml`)

  * Runs on push to `main` (and manual `workflow_dispatch`).
  * SSHes into EC2 using GitHub secrets.
  * Pulls latest code and runs:

    ```bash
    docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
    ```
  * Rebuilds and restarts containers in place.

Branch protection rules require the CI checks to pass before merging into `main`, so only healthy builds are deployed.

---

## Where to look next

* `modules/user-service/README.md` – auth, profile, and error handling.
* `modules/movie-service/README.md` – search, genres, TMDb seeding, and indexing.
* `modules/rating-service/README.md` – ratings, watchlist, and engagement model.
* `modules/gateway/README.md` – routes and aggregated catalog endpoints.
* `frontend/README.md` – Next.js UI and how it talks to the gateway.

