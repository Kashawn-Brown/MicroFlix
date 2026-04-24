# k6 load tests

Load testing for MicroFlix — scenarios mirror what the browser actually hits on
hot pages, so baseline vs post-migration numbers compare like for like.

## Scenarios

| File | Page | Request shape |
|---|---|---|
| `scenarios/watchlist-baseline.js` | `app/watchlist/page.tsx` (pre-migration) | 1 fetch for engagements + N parallel fetches for movie details (true 1+N) |
| `scenarios/watchlist-aggregated.js` | `app/watchlist/page.tsx` (post-migration) | 1 fetch to `/api/v1/catalog/watchlist`; gateway fans out + joins |
| `scenarios/movie-detail-baseline.js` | `app/movies/[id]/page.tsx` + `components/movie-actions.tsx` (pre-migration) | 4 concurrent fetches when authed: movie + summary (SSR) + my-rating + in-watchlist (CSR) |
| `scenarios/movie-detail-aggregated.js` | same page (post-migration) | 2 fetches to `/api/v1/catalog/movies/{id}`: anonymous (SSR) + authed (CSR) |

Baseline vs aggregated scripts are deliberately paired — same load shape (iter/sec,
duration, VUs), same `page_load_duration` Trend, same test identity and seeded
data. That's what makes the median-of-3 before/after comparison in
`docs/benchmarks.md` fair.

Each scenario:
- Uses `constant-arrival-rate` executor for a stable 60s steady state
- Logs in (or registers first) a single loadtest user in `setup()` and seeds
  ~10 watchlist entries so the 1+N fan-out has a realistic N to exercise
- Records a custom `page_load_duration` Trend alongside k6's built-in
  `http_req_duration` so docs/benchmarks.md can report per-page-load latency,
  not just per-endpoint latency

## Running

The Docker stack must be up first:

```bash
cd docker
docker compose up -d
```

### From the host (bare k6 install)

```bash
k6 run -e BASE_URL=http://localhost:8081 k6/scenarios/watchlist-baseline.js
k6 run -e BASE_URL=http://localhost:8081 k6/scenarios/movie-detail-baseline.js
k6 run -e BASE_URL=http://localhost:8081 k6/scenarios/watchlist-aggregated.js
k6 run -e BASE_URL=http://localhost:8081 k6/scenarios/movie-detail-aggregated.js
```

### From a k6 container on the compose network

The scripts default to `BASE_URL=http://gateway:8081` for this path. The compose
network name is derived from the compose project name, which here is `docker`,
so the network is `docker_default`. Verify with `docker network ls` if in doubt.

```bash
docker run --rm --network=docker_default \
  -v "$(pwd)/k6:/scripts" \
  grafana/k6 run /scripts/scenarios/watchlist-baseline.js

docker run --rm --network=docker_default \
  -v "$(pwd)/k6:/scripts" \
  grafana/k6 run /scripts/scenarios/movie-detail-baseline.js

docker run --rm --network=docker_default \
  -v "$(pwd)/k6:/scripts" \
  grafana/k6 run /scripts/scenarios/watchlist-aggregated.js

docker run --rm --network=docker_default \
  -v "$(pwd)/k6:/scripts" \
  grafana/k6 run /scripts/scenarios/movie-detail-aggregated.js
```

Add `--summary-export=/scripts/results/<scenario>-run<N>.json` to save a
summary JSON per run — `docs/benchmarks.md` tables are read from those files.
On Windows Git Bash, prefix the docker command with `MSYS_NO_PATHCONV=1` so
the `/scripts/...` paths don't get mangled into `C:/Program Files/Git/scripts`.

## Methodology

Baselines are captured as **median-of-3** separate runs, not a single run. Local
Docker p95 swings run-to-run enough that single-run numbers would be partially
noise in the before/after comparison. See `docs/benchmarks.md` for the full
measurement story.

The k6 run should also appear as a per-service request-rate spike on the
Grafana **MicroFlix Overview** dashboard (http://localhost:3001, admin/admin)
— a working end-to-end smoke test of the observability stack from Branch 1.
