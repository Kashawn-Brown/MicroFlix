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

## Piece 2 — migration and re-measurement

### What shipped

- **Movie-detail** migrated onto `GET /api/v1/catalog/movies/{id}` (already existed from an earlier gateway pass). `app/movies/[id]/page.tsx` does one anonymous SSR fetch; `components/movie-actions.tsx` does one authed CSR fetch. The gateway fans out to movie-service + rating-service internally via `Mono.zip` and short-circuits the `me` section server-side when no Authorization header is present, so the SSR path skips the authed downstream calls entirely.
- **Watchlist** migrated onto a new `GET /api/v1/catalog/watchlist` endpoint. `CatalogService.getWatchlist(authHeader)` calls rating-service for engagements, short-circuits empty lists, then hits a new `GET /api/v1/movies/batch?ids=...` endpoint on movie-service for one-shot hydration. The join is a pure static helper (`CatalogService.joinWatchlist`) covered by unit tests — it preserves rating-service's `addedAt`-desc order and drops engagements whose movie row has gone missing.
- The movie-service batch endpoint is capped at 50 ids and preserves input-id order (the default `findAllById` return is DB-order, not input-order — the service explicitly re-sorts via a `Map<Long, Movie>` lookup). Covered by `MovieServiceBatchTest` (ordering, drops, empty, null, over-cap, at-cap) and a `@SpringBootTest` integration test against H2 in Postgres-compat mode.

New k6 scripts in `k6/scenarios/`:

- `movie-detail-aggregated.js` — 2 calls per iteration (anonymous SSR + authed CSR), same 20 iter/sec load shape as the baseline
- `watchlist-aggregated.js` — 1 call per iteration, same 5 iter/sec load shape as the baseline

### Piece 2 measurements — `page_load_duration`, median-of-3

#### Movie-detail — 4 direct calls → 2 aggregated calls

| Metric | Baseline median | Aggregated run 1 | Aggregated run 2 | Aggregated run 3 | **Aggregated median** | Δ vs baseline |
|---|---|---|---|---|---|---|
| page_load p50 | 4 ms | 7 ms | 5 ms | 4 ms | **5 ms** | +1 ms |
| page_load p90 | 5 ms | 13 ms | 7 ms | 6 ms | **7 ms** | +2 ms |
| page_load p95 | 5 ms | 16 ms | 8 ms | 7 ms | **8 ms** | +3 ms |
| page_load max | 15 ms | 46 ms | 23 ms | 19 ms | **23 ms** | +8 ms |
| http_req p50 | 2.69 ms | 6.79 ms | 4.88 ms | 3.95 ms | **4.88 ms** | +2.19 ms |
| http_req p95 | 4.17 ms | 15.25 ms | 6.84 ms | 5.99 ms | **6.84 ms** | +2.67 ms |
| iterations | 1200 | 1201 | 1201 | 1201 | **1201** | — |
| http_reqs/sec | 80.0 | 40.4 | 40.1 | 40.1 | **40.1** | **–50%** |
| errors | 0 | 0/2416 | 0/2416 | 0/2416 | **0** | — |

Run 1 is visibly cold-cache — that's first-run-after-restart noise the median-of-3 is designed to absorb.

#### Watchlist — 1+N (12 total) → 1 aggregated

| Metric | Baseline median | Aggregated run 1 | Aggregated run 2 | Aggregated run 3 | **Aggregated median** | Δ vs baseline |
|---|---|---|---|---|---|---|
| page_load p50 | 11 ms | 12 ms | 10 ms | 10 ms | **10 ms** | –1 ms |
| page_load p90 | 14.1 ms | 15 ms | 13 ms | 13 ms | **13 ms** | –1.1 ms |
| page_load p95 | 16 ms | 17 ms | 14 ms | 14 ms | **14 ms** | **–2 ms (–12%)** |
| page_load max | 29 ms | 43 ms | 20 ms | 18 ms | **20 ms** | –9 ms |
| http_req p50 | 3.27 ms | 11.91 ms | 10.09 ms | 9.7 ms | **10.09 ms** | +6.82 ms |
| http_req p95 | 4.97 ms | 17 ms | 13.79 ms | 13.86 ms | **13.86 ms** | +8.89 ms |
| iterations | 301 | 301 | 300 | 301 | **301** | — |
| http_reqs/sec | 60.2 | 5.2 | 5.2 | 5.2 | **5.2** | **–91%** |
| errors | 0 | 0/315 | 0/314 | 0/315 | **0** | — |

### Reading the numbers honestly

**Movie-detail is slightly slower after the migration** (p95 5 ms → 8 ms). This is not a surprise once you look at what each version is actually doing:

- Baseline fires 4 parallel *simple* downstream calls (PK lookup on movies, summary aggregate on ratings, single-row my-rating, single-row watchlist boolean). Each downstream call is ~2–4 ms; the `http.batch` wall-clock is bounded by the slowest of 4.
- Aggregated fires 2 parallel *compound* gateway calls. Each compound call is itself `Mono.zip` over 3 parallel downstream fetches — so every aggregated request's latency is already "slowest of 3" before the two aggregated calls race each other.

In local Docker where inter-container RTT is ~0.3 ms, the gateway hop doesn't buy anything back. In a cross-AZ deployment where client→gateway is 50–200 ms but gateway→service is 1 ms, the math flips: consolidating 4 cross-AZ round-trips into 2 cross-AZ round-trips (with the fan-out happening intra-AZ) would visibly win. The local measurement isn't wrong — it's just measuring in the regime where the architectural value is invisible.

The `http_reqs/sec` column is where the migration's value is already visible in these numbers: 80 req/sec → 40 req/sec at the same page-load rate means the client is generating half the gateway traffic per page view. That's capacity headroom — the same machine can serve twice as many page loads before it saturates anything.

**Watchlist is slightly faster at the tail** (p95 16 ms → 14 ms, –12%) and **dramatically fewer requests** (60 req/sec → 5 req/sec, –91%).

- Baseline has a sequential engagement fetch (~3 ms) followed by an 11-way parallel fan-out whose tail is bounded by the slowest of 11 — more parallel fetches mean a wider tail. Total p95 was 16 ms.
- Aggregated collapses both steps into one call that does the fan-out reactively on the gateway (2 downstream: engagements + movie-batch). The per-request latency is higher (~10 ms vs ~3 ms) because it's doing more work per request, but the *page_load* number — which is what the user feels — came down because we cut the sequential engagement→movies gap that the baseline couldn't avoid.

The watchlist wins here were real even in local Docker: the baseline's "1 sequential + N parallel" shape had a structural round-trip that the aggregation eliminates, independent of network cost.

### Methodology — `http.batch` over-models browser parallelism

Both baseline and aggregated movie-detail scripts fire the SSR-analogue and CSR-analogue requests in a single `http.batch`, i.e. fully parallel. Real browsers don't do that. The actual timeline is:

1. Browser sends page request.
2. Next.js server runs the page's async body, fires the SSR-side fetches, waits for them, renders HTML, returns.
3. Browser receives HTML and renders it. Page is *visible* here.
4. React hydrates `MovieActions`; its `useEffect` fires the CSR-side fetches.
5. Authed rating/watchlist section fills in.

The CSR fetches can't start until after the SSR response lands and hydration runs. So the real user-perceived "fully loaded" time is closer to `(SSR fan-out) + (hydration gap) + (CSR fan-out)`, not `max(SSR, CSR)`.

Keeping `http.batch` in both scripts preserves apples-to-apples comparability — both before and after numbers are wrong by the same factor, and the Δ stays honest. A more realistic model would fire the calls sequentially and accept that the baseline's SSR-vs-CSR structure means the aggregation saves 2 cross-process round-trips (1 on each side), which in local Docker is still tiny but would matter more on real networks.

Left as future work — calling out here so the numbers above aren't over-interpreted.

### Natural extension — profile/ratings

`app/profile/ratings/page.tsx` has the same 1+N shape as the pre-migration watchlist: one `fetchMyRatings` call followed by a parallel fan-out of `fetchMovieById` per rating. The same aggregation treatment would apply cleanly — add `GET /api/v1/catalog/my-ratings` at the gateway, call rating-service for the user's ratings, hydrate via the movie-service batch endpoint, return `[{movie, rate, ...}]` already joined. Not in scope for Branch 3, but earmarked as the obvious next page to consolidate.

A related improvement surfaced in passing — the profile page shows a rating count that includes orphans (ratings whose movie row went missing), so it can disagree with the list which drops them. A `GET /api/v1/catalog/my-ratings/count` endpoint (or making the list endpoint also return the resolvable count) would make the two consistent. Also not in scope here.

### Grafana screenshots

*TODO — file under `docs/screenshots/dashboards/` during Piece 3 wrap. The k6 runs did show as per-service request-rate spikes on the MicroFlix Overview dashboard; notable shape change is movie-service request rate dropping from the 1+N tall spike under the watchlist baseline to a flat ≈5 req/sec matching the iteration rate under the aggregated scenario — the fan-out moved inside the gateway and is no longer visible from the client's perspective on the per-service graphs.*
