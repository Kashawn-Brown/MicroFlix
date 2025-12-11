# MicroFlix

MicroFlix is a microservices-based movie platform I’m building to practice **production-minded backend development** and full-stack integration.

The system is split into small Spring Boot services (users, movies, ratings) behind a Spring Cloud Gateway, with a Next.js frontend on top and a Docker / AWS EC2 deployment that feels close to a real-world setup.

---

## Architecture overview

**Modules:**

- `modules/user-service`  
  - User registration, login (JWT), `/users/me`, profile update, and password change.

- `modules/movie-service`  
  - Movie metadata (title, overview, year, genres, poster/backdrop) with search/filter/sort + pagination and TMDb-based seeding.

- `modules/rating-service`  
  - Movie ratings (1–10 scale with 0.1 increments, stored as `rating_times_ten`) plus a watchlist feature via a generic `engagements` table.

- `modules/gateway`  
  - Spring Cloud Gateway entrypoint. Routes traffic to the microservices and exposes **aggregated catalog endpoints** for the frontend (e.g. `/api/v1/catalog/movies/{id}`).

- `modules/discovery`  
  - Eureka discovery server so services can find each other by name (`lb://user-service`, `lb://movie-service`, etc.).

- `frontend/`  
  - Next.js App Router app (TypeScript + Tailwind CSS) for auth, browsing movies, rating, and watchlist.

- `docker/`  
  - Docker Compose setup for running the full stack locally and in AWS.

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
