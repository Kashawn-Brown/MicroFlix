
---

# User Service

This is the **user-service** for Microflix. It handles user registration, login with JWT, 
and profile management so other services can reliably identify and trust the current user.

It utilizes: Spring Boot 3, Java 21, Postgres, Flyway, and a thin controller + service + repository stack.

This setup runs the **user-service** and **Postgres 18** database in Docker along with the rest of the Microflix stack, so you don’t need a local DB installed.

### Prerequisites

* Docker / Docker Desktop
* Java 21 + Maven (only needed if you still run the app locally outside Docker)

### What’s running

From `docker/docker-compose.yml`, I have two main services:

* **user-db**

    * Image: `postgres:18`
    * Host port: `5434` → container port `5432`
    * Database: `userdb`
    * Main table: `users`
    * Username/password: `user` / `user`
    * Data is stored in a named volume `pg_user_data`, so it survives container restarts.
    * Schema created by Flyway migration `V1__init_users.sql`

* **user-service**

    * Built from `modules/user-service/Dockerfile` (multi-stage: Maven build + slim Java 21 runtime).
    * Exposed on `http://localhost:8082`
    * Connects to Postgres using `jdbc:postgresql://user-db:5432/userdb`.
    * Registers with Eureka so the **gateway** can route `/api/v1/users/**` & `/api/v1/auth/**`calls to it.

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

On startup, Flyway runs `V1__init_users.sql` and creates a `users` table with:

- `id UUID PRIMARY KEY` – stable user identifier used across the system
- `email VARCHAR(255) NOT NULL UNIQUE` – unique email used for login
- `password_hash VARCHAR(72) NOT NULL` – bcrypt (or similar) hash of the user’s password
- `display_name VARCHAR(100)` – optional name shown in the UI
- `roles VARCHAR(100) NOT NULL DEFAULT 'USER'` – comma-separated roles (e.g. `USER`, `ADMIN`)
- `is_active BOOLEAN NOT NULL DEFAULT TRUE` – soft-active flag for disabling accounts
- `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` – when the user was created
- `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` – last time the user record was updated
- `last_login_at TIMESTAMPTZ` – timestamp of the last successful login (nullable)

There is an index on `email` (`idx_users_email`) so login and user lookups by email stay fast as the table grows.

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

# (or: docker compose up --build user-db user-service)
```

Once things are up, I expect to see logs for:

* `pg-user` (Postgres):
  `database system is ready to accept connections`
* `user-service`: the Spring Boot startup banner + Flyway migrations +
  “`Started UserServiceApplication...`”
* \+ The rest of the Microflix stack starting up

At this point:

* `user-service` is available directly at: `http://localhost:8082`
* Through the gateway, user endpoints live under: `http://localhost:8081/api/v1/users` or `http://localhost:8081/api/v1/auth`

---

## API endpoints

I use these calls to confirm everything is working end-to-end via the gateway.

### **Register a user**

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "displayName": "Test User"
  }'
```

I expect a `200 OK` with a JSON body that includes a JWT `token` and basic user info.

### **Login user**

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

From this response, I grab the `token` field.

### **Get the current user**

```bash
curl http://localhost:8081/api/v1/users/me \
  -H "Authorization: Bearer <PASTE_TOKEN_HERE>"
```

I expect a `200 OK` with something like:

```json
{
  "id": "...",
  "email": "test@example.com",
  "displayName": "Test User",
  "roles": ["ROLE_USER"]
}
```

### Stopping and resetting the environment

To stop the containers (but keep the DB data):

```bash
docker compose down
```

If I want a completely fresh database (drop all data and rerun migrations):

```bash
docker compose down -v   # removes containers + volumes
docker compose up --build
```

That gives me a clean state with Flyway recreating the schema from scratch.


---


## Testing

I keep the `user-service` covered by a mix of service-level and controller-level tests so I can refactor with confidence.

### Prerequisites

- Java 21 installed and on my `PATH`.
- Maven wrapper available at the project root (`./mvnw`).
- Don't need Docker or Postgres running to execute the current unit tests; they use mocks and run entirely in memory.

### Running user tests 

From the project root, navigate to the user-service directory:

```bash
cd modules\user-service
````

I can run all tests with:

```bash
mvn test
````

I should see `BUILD SUCCESS` if everything passes.

### Running only user-service tests

If I only want to run tests for `user-service` (in a multi-module setup), I can do:

```bash
./mvnw test -pl user-service
```

This tells Maven to only build and test the `user-service` module.

### What the tests cover right now

Right now the tests focus on the core auth and profile flows:

* **AuthService tests**

  * Registering a new user and hashing the password.
  * Logging in with valid credentials and returning an `AuthResponse` (JWT token, email, displayName, roles).
  * Rejecting invalid credentials.

* **ProfileService tests**

  * Loading the current user via `CurrentUser` and returning profile info (`/users/me`).
  * Changing the password only when the old password matches.

* **Controller tests**

  * `AuthController` and `ProfileController` mapping HTTP requests to the right service calls.
  * Converting a Spring Security `Authentication` into a `CurrentUser`.
  * Returning the expected status codes and DTOs for happy-path flows.

As I add new behavior (for example, new profile fields or more detailed error handling), I add or update tests alongside the changes.

### Troubleshooting

* If tests fail with missing classes or beans, I make sure I’m running from the project root with `./mvnw test` so Maven picks up the full multi-module context.
* If a test fails unexpectedly, I check the stack trace for which service or controller threw the exception and update either the test or the implementation accordingly.

---

## Observability

The user-service exposes basic health information via Spring Boot Actuator.

- **Health check**

  - `GET /actuator/health`
  - Returns a simple JSON status, e.g.:

    ```json
    { "status": "UP" }
    ```

  This endpoint is intended to be used by Docker, future load balancers, or simple uptime checks to verify that the service is running.

> In production (AWS), this endpoint is available on the internal Docker network only. To check it manually you can SSH into the EC2 instance and run:
>
> ```bash
> curl http://user-service:8082/actuator/health
> ```

(We deliberately do **not** expose Actuator endpoints publicly to the internet.)

## API Documentation (OpenAPI / Swagger)

The user-service exposes its HTTP API contract via Springdoc OpenAPI.

- **OpenAPI JSON**

  - `GET /v3/api-docs`

- **Swagger UI**

  - `GET /swagger-ui/index.html`

These docs are generated automatically from the `@RestController` classes and DTOs.  
They list endpoints such as:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET  /api/v1/users/me`
- `PATCH /api/v1/users/me`
- `PATCH /api/v1/users/me/password`
- `GET  /api/v1/users/health`
- `GET  /api/v1/admin/ping`

> In production, Swagger UI is primarily for internal use (e.g., via SSH tunnel or internal tools) and should not be exposed to the open internet.

---

## Error handling

* Invalid credentials:

  * When I call `/api/v1/auth/login` with a bad email/password combination, the service returns an error response.
  * The controller and security layer work together so that invalid credentials don’t leak details about which part was wrong (email vs password).

* Duplicate registration:

  * When I try to register with an email that already exists, the service rejects the request and does not create a second user.
  * This keeps the `email` field unique and avoids inconsistent account state.

* Unauthorized access:

  * All profile-related endpoints (like `/api/v1/users/me`, update profile, change password) are protected by JWT-based auth.
  * If I hit these endpoints without a valid `Authorization: Bearer <token>` header, the service returns an appropriate 401-style response instead of user data.

As I refine the API, the goal is to keep controller code simple and move more of the error mapping into shared handlers so clients see clear, consistent error formats across endpoints.

---

## Authentication & error shape

The user-service issues JWTs from `/api/v1/auth/login` and `/api/v1/auth/register`.  
On successful login, I get back a token that I pass to other services as:

```http
Authorization: Bearer <JWT>
````

Security rules:

* `POST /api/v1/auth/**` and `/api/v1/users/health` are **public**.
* All other `/api/**` endpoints require a valid JWT.

If I hit a protected endpoint without a token, or with an invalid/expired token, Spring Security calls my `JwtAuthenticationEntryPoint` and I get a `401 Unauthorized` response using Spring’s `ProblemDetail` format, for example:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required to access this resource.",
  "path": "/api/v1/users/me",
  "timestamp": "2025-11-20T12:34:56.789Z"
}
```

Domain / validation errors inside the service also use `ProblemDetail`, so clients always see a consistent error shape.



---

## Future plans

Later, I plan to extend user-service to:

* Add user-facing flows like password reset, email verification, and possibly multi-factor auth.
* Standardize error responses (using `ProblemDetail` or a shared error envelope) so all services return a consistent JSON format for failures.
* Introduce more fine-grained roles and permissions once there are admin/moderation features.
* Expose additional profile fields and preferences that other services (like ratings or recommendations) can use.

These are intentionally kept out of the initial MVP so the core auth + profile flows stay stable and easy to reason about first.

---