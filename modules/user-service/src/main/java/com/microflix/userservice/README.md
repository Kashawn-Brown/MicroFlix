
---

## 🐳 Local Dev: Running `user-service` + Postgres with Docker Compose
This setup runs the **user-service** and **Postgres 18** in Docker, so you don’t need a local DB installed.

### Prerequisites

* Docker / Docker Desktop
* Java 21 + Maven (only needed if you still run the app locally outside Docker)

### What’s running

From `docker/docker-compose.yml`, I have two main services:

* **user-db**

    * Image: `postgres:18`
    * Host port: `5434` → container port `5432`
    * Database: `userdb`
    * Username/password: `user` / `user`
    * Data is stored in a named volume `pg_user_data`, so it survives container restarts.

* **user-service**

    * Built from `modules/user-service/Dockerfile` (multi-stage: Maven build + slim Java 21 runtime).
    * Exposed on `http://localhost:8082`
    * Connects to Postgres using `jdbc:postgresql://user-db:5432/userdb`.
    * In this Docker setup I disable Eureka via env vars:

        * `EUREKA_CLIENT_ENABLED=false`
        * `SPRING_CLOUD_DISCOVERY_ENABLED=false`

### How I start the stack

From the `docker/` directory:

```bash
# Stop any old containers for this compose file
docker compose down

# Build images and start Postgres + user-service
docker compose up --build
# (or: docker compose up --build user-db user-service)
```

Once things are up, I expect to see logs for:

* `pg-user` (Postgres):
  `database system is ready to accept connections`
* `user-service`: the Spring Boot startup banner + Flyway migrations +
  “`Started UserServiceApplication...`”

At this point, `user-service` is available at:

* `http://localhost:8082`

### Sanity check: basic auth & profile flow

I use these calls to confirm everything is working end-to-end.

### **1️⃣ Register**

```bash
curl -X POST http://localhost:8082/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "displayName": "Test User"
  }'
```

I expect a `200 OK` with a JSON body that includes a JWT `token` and basic user info.

### **2️⃣ Login**

```bash
curl -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

From this response, I grab the `token` field.

### **3️⃣ Get the current user (`/users/me`)**

```bash
curl http://localhost:8082/api/v1/users/me \
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