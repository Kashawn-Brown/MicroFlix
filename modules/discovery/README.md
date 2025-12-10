# Discovery Service

The `discovery` is a simple **Eureka server** used for service discovery in MicroFlix.

All other backend services (`user-service`, `movie-service`, `rating-service`, `gateway`) register themselves with it and look up each other by **service ID** instead of hardcoded hostnames.

---

## Responsibilities

- Acts as a central registry for microservices.
- Enables client-side load balancing via `lb://service-name` URIs in the gateway and other clients.
- Provides a small web UI for viewing registered instances.

---

## Running locally

Discovery runs as part of the Docker stack:

```bash
cd docker
docker compose up --build
````

* Eureka dashboard: `http://localhost:8761`

From there you can see which services are registered and their health/status.

To run it standalone:

```bash
cd modules/discovery
mvn spring-boot:run
```

It will start on port **8761** by default.

---

