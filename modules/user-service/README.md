# User Service

The `user-service` is responsible for **user accounts and authentication**:

- Register new users
- Log in and issue JWTs
- Expose `/users/me` for the current user
- Update profile details
- Change password

It’s built with Spring Boot 3, Spring Security 6 (JWT), and PostgreSQL.

---

## Responsibilities

- **Authentication**
  - `POST /api/v1/auth/register` – create a new user (email, displayName, password)
  - `POST /api/v1/auth/login` – authenticate and return a JWT
- **Current user**
  - `GET /api/v1/users/me` – return the current user’s profile
  - `PUT /api/v1/users/me` – update profile fields (e.g., displayName)
  - `POST /api/v1/users/me/change-password` – change password with old/new credentials

The service is **stateless**: every protected request must carry `Authorization: Bearer <JWT>`.

---

## Tech stack

- Java 21, Spring Boot 3
- Spring Web
- Spring Security (JWT with HS256)
- Spring Data JPA + PostgreSQL
- Flyway for DB migrations
- JUnit 5 + Mockito

---

## Running locally

The easiest way is via the root `docker-compose`:

```bash
cd docker
docker compose up --build
````

This starts `user-service` on port **8082** with a dedicated Postgres database (`user-db`).

To run just this service (with a local Postgres instead), you can use:

```bash
cd modules/user-service
mvn spring-boot:run
```

Configure DB connection via `application.yml` or environment variables.

---

## Security & JWT

* Uses Spring Security 6 with a stateless `SecurityFilterChain`.
* A JWT is issued on successful login and must be sent on each protected request:

  * `Authorization: Bearer <token>`
* The JWT is validated locally in `user-service` using a shared secret.
* A custom authentication entry point returns **401 ProblemDetail** responses when the token is missing or invalid.

---

## Error handling

`user-service` uses a global `@RestControllerAdvice` to convert exceptions into `ProblemDetail` JSON:

* `IllegalArgumentException` → **400 Bad Request**
* `MethodArgumentNotValidException` (Bean Validation) → **400 Validation Failed** with a per-field `errors` map
* `Exception` (fallback) → **500 Internal Error** with a safe generic message

All error responses use the same basic shape (status, title, detail) so the frontend can handle them consistently.

---

## Observability & API docs

* Health:

  * `GET /actuator/health` → `{ "status": "UP" }`
* OpenAPI JSON:

  * `GET /v3/api-docs`
* Swagger UI:

  * `GET /swagger-ui/index.html`

These endpoints are accessible on `http://localhost:8082` when running locally. In production they’re reachable on the internal network (not exposed directly to the internet).

---