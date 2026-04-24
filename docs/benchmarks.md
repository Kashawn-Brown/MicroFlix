# Benchmarks — k6 load tests of hot frontend pages

Branch: `performance/benchmarks-and-aggregation` · Catalog size at measurement: **~3.7k movies** (unchanged from Branch 2) · Stack: local Docker compose, post-V6 indexes, Prometheus/Grafana observability live.

All measurements taken against the live compose stack after the Branch 2 merge. "Baseline" here means the already-indexed, already-instrumented state — not pre-V4 tables. The work of this branch is measuring the *request shape* from the browser down through the gateway, not the DB layer (Branch 2 handled that).

---

## Framing — why these two pages

Two frontend pages drive meaningfully more per-page HTTP traffic than a single fetch, and both have plausible aggregation targets at the gateway:

1. **Watchlist** (`app/watchlist/page.tsx`) — true 1+N. One fetch for the engagement list, then `Promise.all(engagements.map(fetchMovieById))` to hydrate each row with movie metadata. Fans across two services (rating-service → movie-service).
2. **Movie detail** (`app/movies/[id]/page.tsx` + `components/movie-actions.tsx`) — 4 concurrent fetches when authed. `Promise.all` for movie + rating summary on the server, and `Promise.all` for my-rating + in-watchlist in the client `MovieActions` component. All four land at roughly the same wall-clock moment from the user's perspective.

The gateway already exposes `GET /api/v1/catalog/movies/{id}`, which fans out to movie-service and rating-service internally via `Mono.zip` and returns one response. Piece 2 will migrate the movie-detail page onto that endpoint and re-measure. The watchlist page doesn't have a matching aggregation endpoint yet — Piece 2 will decide whether to add one or take a different approach.

---

## Methodology

### Runner

k6 in a Docker container attached to the compose network:

```bash
docker run --rm --network=docker_default \
  -v "$PWD/k6:/scripts" \
  grafana/k6 run /scripts/scenarios/watchlist-baseline.js
```

Scripts live in `k6/scenarios/`, shared helpers in `k6/lib/`. See `k6/README.md` for the bare-k6 alternative and run details.

### Load shape

Both scenarios use the `constant-arrival-rate` executor — fixed iterations/sec regardless of VU response time, so we measure latency at a defined offered load rather than letting k6 throttle itself as the stack slows. 60s steady state, no ramp.

- Watchlist: 5 iter/sec (≈ 60 req/sec, since each iteration is 1 + 11 = 12 requests)
- Movie detail: 20 iter/sec (≈ 80 req/sec, 4 requests per iteration)

Rates chosen to exercise the stack without saturating local Docker; max-throughput / soak work isn't the goal of Piece 1.

### Median-of-3

Each scenario runs **3 separate times** and the reported number is the median of the three. Local Docker p95 swings run-to-run enough (GC, container resource juggling, whatever else the machine is doing) that single-run numbers would be partially noise in the before/after comparison. Median-of-3 is cheap and the writeup gets to say so.

Note on iterations-within-run vs runs-across-time: k6's per-iteration percentiles already average across ~300–1200 iterations inside one run, which smooths iteration-level noise. What median-of-3 smooths is *inter-run* noise — the "the whole machine was a bit warmer / slower this minute" kind of variance that iteration averaging can't capture.

### Fixed test data, intentionally

- Test user: single `loadtest@microflix.local` registered-or-logged-in via k6's `setup()`; same identity every run.
- Fixed movie id `524` for the movie-detail scenario (same reference id as Q6/Q8 in `docs/explain-analyze.md`, for continuity).
- Watchlist pre-seeded with the first 10 movie ids from page 0 of the default browse, plus 524.

Fixed ids are intentional for baseline reproducibility. Realistic traffic would vary the id across iterations; the randomization step can come later when it's earning its keep.

### Per-page-load metric

k6's built-in `http_req_duration` measures one HTTP request at a time, which is the wrong unit for answering "does the page feel slow." Both scripts record a custom `page_load_duration` Trend that wraps the full block — the watchlist's 1+N fan-out, or the movie-detail's 4-concurrent `http.batch`. That's the number to compare across before/after in Piece 2.

---

## Piece 1 baseline measurements

All checks passed 100% across all 6 runs (0 failed requests). Request counts per iteration matched the expected shape exactly: 12 for watchlist (1 engagements + 11 movie fetches), 4 for movie-detail.

### Watchlist — 1+N

| Metric | Run 1 | Run 2 | Run 3 | **Median** |
|---|---|---|---|---|
| iterations | 301 | 300 | 301 | **301** |
| http_reqs/sec | 60.2 | 60.1 | 60.3 | **60.2** |
| page_load p50 | 12 ms | 11 ms | 11 ms | **11 ms** |
| page_load p90 | 16 ms | 14.1 ms | 13 ms | **14.1 ms** |
| page_load p95 | 18 ms | 16 ms | 14 ms | **16 ms** |
| page_load max | 73 ms | 29 ms | 21 ms | **29 ms** |
| http_req_duration p50 | 3.43 ms | 3.27 ms | 3.12 ms | **3.27 ms** |
| http_req_duration p95 | 5.27 ms | 4.97 ms | 4.26 ms | **4.97 ms** |
| errors | 0 / 3626 | 0 / 3614 | 0 / 3626 | **0** |

**Reading the numbers.** At 5 page-loads/sec, one page load takes a median 11 ms and a p95 16 ms end-to-end. Per-request latency is median 3.27 ms / p95 4.97 ms — so the 1+N pattern is adding roughly 3× the single-request latency at p50, with modest widening at p95. That's consistent with `http.batch` firing 11 parallel GETs at the movie-service and being bounded by the slowest, plus the preceding sequential engagement fetch.

The run 1 outlier on `max` (73 ms vs 29/21 for runs 2–3) is a single iteration's tail, not a distributional shift — the p95 tracks the rest of the runs cleanly. That's exactly the sort of noise median-of-3 is designed to absorb.

### Movie detail (authed) — 4 concurrent

| Metric | Run 1 | Run 2 | Run 3 | **Median** |
|---|---|---|---|---|
| iterations | 1201 | 1200 | 1200 | **1200** |
| http_reqs/sec | 80.1 | 79.9 | 80.0 | **80.0** |
| page_load p50 | 4 ms | 4 ms | 4 ms | **4 ms** |
| page_load p90 | 5 ms | 5 ms | 5 ms | **5 ms** |
| page_load p95 | 5 ms | 5 ms | 5 ms | **5 ms** |
| page_load max | 14 ms | 20 ms | 15 ms | **15 ms** |
| http_req_duration p50 | 2.90 ms | 2.67 ms | 2.69 ms | **2.69 ms** |
| http_req_duration p95 | 4.22 ms | 4.01 ms | 4.17 ms | **4.17 ms** |
| errors | 0 / 4818 | 0 / 4814 | 0 / 4814 | **0** |

**Reading the numbers.** At 20 page-loads/sec, page load is median 4 ms / p95 5 ms. Per-request latency is median 2.69 ms / p95 4.17 ms — so the 4-concurrent batch is bounded by the slowest of 4, which tracks single-request p95 closely. Distribution is noticeably tighter than watchlist, which makes sense: 4 parallel fetches, no preceding sequential step, and three of the four (movie, summary, in-watchlist) return small payloads.

These numbers are the pre-migration shape. Piece 2 moves the page onto the gateway's existing `/api/v1/catalog/movies/{id}` aggregation endpoint, collapsing 4 parallel client-initiated requests into 1 client-initiated request that the gateway fans out internally. The re-measurement will compare the same `page_load_duration` Trend under the same load shape.

### Grafana smoke — observability works end-to-end with k6-generated load

Each k6 run showed up as a request-rate spike on the **MicroFlix Overview** dashboard (Grafana, `http://localhost:3001`, provisioned in Branch 1). Notable lanes during the runs:

- **Watchlist scenario:** movie-service request rate ~11× the rating-service rate (the 1+N pattern visible directly in the dashboard), gateway rate tracking the sum.
- **Movie-detail scenario:** movie-service rate ≈ 1× iteration rate, rating-service rate ≈ 3× (summary + my-rating + in-watchlist), gateway rate tracking the sum.

This confirms the Branch 1 observability pipeline (Micrometer → Prometheus → Grafana) is wired correctly for k6-generated traffic — so the same dashboards are available for before/after comparison when Piece 2 lands.

Screenshot: *TODO — to be captured and filed under `docs/screenshots/dashboards/` during Piece 1 wrap.*

---

## Piece 2 — migration and re-measurement *(next kickoff)*

Planned but not yet done:

- **Movie-detail:** migrate the page (SSR + `MovieActions`) onto `GET /api/v1/catalog/movies/{id}`. Re-run `movie-detail-baseline.js` unchanged; compare `page_load_duration` median-of-3 before/after. The aggregation endpoint does the same 4 downstream calls internally, so the question is whether gateway-side parallelism + one TCP round-trip beats 4 browser-side concurrent round-trips across wall-clock latency.
- **Watchlist:** decide the shape. No matching aggregation endpoint exists today. Options: add one at the gateway, change the server-side vs client-side split, or leave the page alone if measurement says the 1+N is already cheap enough to not justify the aggregation complexity. Decision will be recorded here before any code changes.
- Grafana before/after screenshots filed under `docs/screenshots/dashboards/`.
