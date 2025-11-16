# Microflix

Microflix is a microservices-based movie platform I’m building to practice production-minded backend development.

Right now the focus is on a solid `user-service` with proper auth, profile management, tests, and a local Docker stack that feels close to how a real system would run.

---

## Stack overview

Current modules:

- `modules/user-service` – user registration, login (JWT), and profile (`/users/me`, update profile, change password).
- `modules/gateway` – Spring Cloud Gateway entrypoint for the API.
- `modules/discovery` – Eureka discovery server.
- `docker/` – Docker Compose setup for running the local stack.

Key technologies:

- Java 21, Spring Boot 3
- Spring Security with JWT (HS256)
- Spring Cloud Gateway + Eureka discovery
- Postgres 18 + Flyway
- Docker + Docker Compose
- JUnit 5 + Mockito for tests

---

## Running the stack with Docker

I use Docker Compose to run the core services together:

- `discovery` (Eureka server) on port **8761**
- `gateway` (API entrypoint) on port **8081**
- `user-service` on port **8082** (behind gateway)
- `user-db` (Postgres 18) with a named volume for data

From the `docker/` directory:

```bash
cd docker
docker compose up --build
