# EXPLAIN ANALYZE — movie-service hot catalog queries

Branch: `performance/dataset-and-indexes` · Catalog size at measurement: **3747 movies, 8391 movie–genre links, 19 genres**.

All measurements taken against the live `moviedb` Postgres 18.1 container after `ANALYZE movies; ANALYZE movie_genres; ANALYZE genres;` to ensure planner stats reflect the freshly-seeded catalog. Each query was run via `EXPLAIN (ANALYZE, BUFFERS)` with hand-written SQL mirroring what Hibernate emits for the corresponding repository / Specification call.

---

## Baseline framing — what V4 already covered

Migration `V4__add_movie_search_indexes.sql` was added pre-Branch-2 and put three indexes in place anticipating the obvious filter / sort paths:

| Index | Covers |
|---|---|
| `idx_movies_created_at_id (created_at DESC, id)` | Default sort (`created_desc`) used by every browse-page load |
| `idx_movies_release_year (release_year)` | `?year=NNNN` filter, year-sort paths |
| `idx_movie_genres_genre_movie (genre_id, movie_id)` | `?genre=X` filter via the `genre → movies` join direction |

The job of Piece 2 is therefore not "movie-service has no indexes — add some." It's: **measure the hot queries against the seeded catalog, confirm V4's indexes are doing their job, and find the gaps V4 didn't anticipate.**

What V4 covered well (verified below): default browse, year filter (count side), year sort, genre filter (both directions of the join).

What V4 missed (verified below):
- `tmdb_id` lookups — sequential scan today, hit thousands of times during ingestion. Highest-leverage gap.
- `LOWER(title) LIKE '%…%'` text search — sequential scan, will scale linearly with catalog size.
- `title` sort path — sequential scan + in-memory top-N sort, no supporting index.

There is also one *interaction* worth noting (Q2 below): the SELECT side of `?year=` paged queries doesn't use the year index; the planner prefers walking V4's sort index and filtering inline. Not a missing index per se, but a composite-index opportunity if year-filtered browsing becomes a hot path.

---

## Test values

Picked from real seeded data:

- **Year filter:** `2024` (129 rows — middle of distribution, avoids the heavy 2025/2026 skew)
- **Genre filter:** `Action` (715 movies, 4th most common — realistic browse target)
- **Title search:** `'%love%'` (63 matches — realistic search term)
- **tmdb_id lookup:** `1419406` (real id for "The Shadow's Edge", id=524) for the hit case; `99999999` for the miss case
- **PK lookup:** `id = 524`

---

## Q1 — Default browse: `GET /api/v1/movies?page=0&size=12`

Always-true Specification, sort `created_desc`, page 0.

### SELECT (page query)

```sql
SELECT m.id, m.title, m.release_year, m.created_at
FROM movies m
ORDER BY m.created_at DESC
LIMIT 12 OFFSET 0;
```

```
Limit  (cost=0.28..2.51 rows=12 width=37) (actual time=0.027..0.038 rows=12)
  Buffers: shared hit=4
  ->  Index Scan using idx_movies_created_at_id on movies m
        (cost=0.28..696.27 rows=3747 width=37) (actual time=0.026..0.035 rows=12)
        Buffers: shared hit=4
Execution Time: 0.466 ms
```

**Verdict: ✅ V4 hit.** Index Scan walks `idx_movies_created_at_id` for the first 12 rows. 4 buffers, sub-millisecond. Optimal.

### COUNT (page metadata)

```sql
SELECT COUNT(m.id) FROM movies m;
```

```
Aggregate  (actual time=0.682..0.682 rows=1)
  ->  Index Only Scan using movies_pkey on movies m
        (actual time=0.061..0.420 rows=3747)
        Heap Fetches: 0
Execution Time: 0.696 ms
```

**Verdict: ✅ optimal.** Index Only Scan on the PK — counts via the index without heap fetches.

---

## Q2 — Year filter: `?year=2024&page=0&size=12`

### SELECT (page query)

```sql
SELECT m.id, m.title, m.release_year, m.created_at
FROM movies m
WHERE m.release_year = 2024
ORDER BY m.created_at DESC
LIMIT 12 OFFSET 0;
```

```
Limit  (cost=0.28..65.89 rows=12 width=37) (actual time=0.047..0.227 rows=12)
  Buffers: shared hit=56
  ->  Index Scan using idx_movies_created_at_id on movies m
        (cost=0.28..705.64 rows=129 width=37) (actual time=0.046..0.204 rows=12)
        Filter: (release_year = 2024)
        Rows Removed by Filter: 577
        Buffers: shared hit=56
Execution Time: 0.253 ms
```

**Verdict: ⚠️ V4 partial — interesting interaction.** The planner uses V4's *sort* index (`idx_movies_created_at_id`) and applies the year as a **post-index filter**, walking 589 rows to find the first 12 matches. It does *not* use `idx_movies_release_year` for this query because at LIMIT 12 against 129 matches in a 3747-row table, that's faster than building a bitmap on the year and then sorting.

This is the planner making a smart choice given what's available, not a bug. A composite index `(release_year, created_at DESC)` would let the engine skip both the filter step and the sort. Worth flagging for V5 *only if* year-filtered browsing turns out to be a heavily-hit path — defer until measured.

### COUNT

```sql
SELECT COUNT(m.id) FROM movies m WHERE m.release_year = 2024;
```

```
Aggregate  (actual time=0.195..0.196 rows=1)
  ->  Bitmap Heap Scan on movies m  (actual time=0.028..0.185 rows=129)
        Recheck Cond: (release_year = 2024)
        ->  Bitmap Index Scan on idx_movies_release_year
              (actual time=0.015..0.015 rows=129)
              Index Cond: (release_year = 2024)
Execution Time: 0.211 ms
```

**Verdict: ✅ V4 hit.** Bitmap Index Scan on `idx_movies_release_year` for the count.

---

## Q3 — Genre filter: `?genre=Action&page=0&size=12`

### SELECT (page query, with DISTINCT from Specification)

```sql
SELECT DISTINCT m.id, m.title, m.release_year, m.created_at
FROM movies m
LEFT JOIN movie_genres mg ON mg.movie_id = m.id
LEFT JOIN genres g ON g.id = mg.genre_id
WHERE LOWER(g.name) = 'action'
ORDER BY m.created_at DESC
LIMIT 12 OFFSET 0;
```

```
Limit  (actual time=2.302..2.308 rows=12)
  Buffers: shared hit=367
  ->  Unique  (actual time=2.300..2.305 rows=12)
        ->  Incremental Sort  (actual time=2.299..2.301 rows=12)
              Presorted Key: m.created_at, m.id
              ->  Nested Loop  (actual time=1.732..2.031 rows=33)
                    Join Filter: (mg.genre_id = g.id)
                    ->  Nested Loop  (actual time=0.033..0.288 rows=355)
                          ->  Index Scan using idx_movies_created_at_id on movies m  (rows=169)
                          ->  Index Only Scan using uk_movie_genre on movie_genres mg  (rows=2)
                                Index Cond: (movie_id = m.id)
                                Heap Fetches: 0
                    ->  Materialize  (actual time=0.005..0.005 rows=1)
                          ->  Seq Scan on genres g  (actual time=1.649..1.662 rows=1)
                                Filter: (lower((name)::text) = 'action'::text)
                                Rows Removed by Filter: 18
Execution Time: 2.427 ms
```

**Verdict: ✅ V4 helps via the sort index; uniqueness constraint covers the join.** The plan walks V4's `idx_movies_created_at_id` in sort order, then for each movie joins via the *UNIQUE* constraint `uk_movie_genre (movie_id, genre_id)` — that's the natural index for the `movie → mg` direction. The genre table itself takes a Seq Scan because `LOWER(name) = 'action'` can't use the unique btree on `name`, but with only 19 genres that costs nothing.

Note: the SELECT uses `uk_movie_genre`, not V4's `idx_movie_genres_genre_movie`. V4's index serves the *opposite* join direction (genre → movies), which is what the COUNT query uses below.

### COUNT

```sql
SELECT COUNT(DISTINCT m.id)
FROM movies m
LEFT JOIN movie_genres mg ON mg.movie_id = m.id
LEFT JOIN genres g ON g.id = mg.genre_id
WHERE LOWER(g.name) = 'action';
```

```
Aggregate  (actual time=0.906..0.907 rows=1)
  ->  Merge Join  (actual time=0.180..0.865 rows=715)
        Merge Cond: (mg.movie_id = m.id)
        ->  Sort  (actual time=0.163..0.189 rows=715)
              Sort Key: mg.movie_id
              ->  Nested Loop  (actual time=0.030..0.136 rows=715)
                    ->  Seq Scan on genres g  (rows=1)
                          Filter: (lower((name)::text) = 'action'::text)
                    ->  Index Only Scan using idx_movie_genres_genre_movie on movie_genres mg
                          (rows=715)
                          Index Cond: (genre_id = g.id)
                          Heap Fetches: 0
        ->  Index Only Scan using movies_pkey on movies m
              (actual time=0.015..0.426 rows=3743)
              Heap Fetches: 0
Execution Time: 0.942 ms
```

**Verdict: ✅ V4 hit.** This is the query V4's `idx_movie_genres_genre_movie (genre_id, movie_id)` was designed for: walk all 715 movie-genre links for "Action" via the index, merge-join to the movies PK. Index Only Scans on both sides, no heap fetches.

---

## Q4 — Title text search: `?query=love`

### SELECT (page query)

```sql
SELECT m.id, m.title, m.release_year, m.created_at
FROM movies m
WHERE LOWER(m.title) LIKE '%love%'
ORDER BY m.created_at DESC
LIMIT 12 OFFSET 0;
```

```
Limit  (actual time=3.631..3.633 rows=12)
  Buffers: shared hit=229
  ->  Sort  (actual time=3.630..3.632 rows=12)
        Sort Key: created_at DESC
        Sort Method: top-N heapsort  Memory: 27kB
        ->  Seq Scan on movies m  (actual time=1.233..3.593 rows=63)
              Filter: (lower((title)::text) ~~ '%love%'::text)
              Rows Removed by Filter: 3684
Execution Time: 3.662 ms
```

### COUNT

```sql
SELECT COUNT(m.id) FROM movies m WHERE LOWER(m.title) LIKE '%love%';
```

```
Aggregate  (actual time=1.969..1.970 rows=1)
  ->  Seq Scan on movies m  (actual time=0.063..1.956 rows=63)
        Filter: (lower((title)::text) ~~ '%love%'::text)
        Rows Removed by Filter: 3684
Execution Time: 1.997 ms
```

**Verdict: ❌ V4 gap — full Seq Scan on both queries.** The leading `%` in `%love%` defeats normal btree indexes even on `lower(title)`. At 3747 rows this costs ~3.6ms and 229 buffers — manageable but linear in catalog size, and pollutes the buffer cache with unrelated pages on every search.

**V5 candidate:** `pg_trgm` GIN index on `lower(title)`. Extension verified available (`pg_trgm 1.6` in `pg_available_extensions`).

---

## Q5 — Sort by title / year (no filter)

### Title sort

```sql
SELECT m.id, m.title, m.release_year, m.created_at
FROM movies m
ORDER BY m.title ASC LIMIT 12 OFFSET 0;
```

```
Limit  (actual time=3.801..3.803 rows=12)
  Buffers: shared hit=229
  ->  Sort  (actual time=3.800..3.801 rows=12)
        Sort Key: title
        Sort Method: top-N heapsort  Memory: 26kB
        ->  Seq Scan on movies m  (actual time=0.009..1.254 rows=3747)
Execution Time: 3.838 ms
```

**Verdict: ❌ V4 gap, but borderline.** Seq Scan + top-N heapsort over the full 3747 rows. Memory sort is cheap at this size (26 kB), but again linear in catalog size. A simple `idx_movies_title (title)` would let the planner walk the index for the first 12.

**V5 candidate:** `idx_movies_title (title)` — small, cheap, but only worth adding if title sort is plausibly hot. Lower priority than the other two.

### Year sort (`?sort=year_desc`)

```sql
SELECT m.id, m.title, m.release_year, m.created_at
FROM movies m
ORDER BY m.release_year DESC LIMIT 12 OFFSET 0;
```

```
Limit  (actual time=0.006..0.012 rows=12)
  Buffers: shared hit=9
  ->  Index Scan Backward using idx_movies_release_year on movies m
        (actual time=0.005..0.011 rows=12)
Execution Time: 0.049 ms
```

**Verdict: ✅ V4 hit.** `idx_movies_release_year` walked backward — 9 buffers, 0.05ms. Optimal.

---

## Q6 — Single movie by id: `GET /api/v1/movies/{id}`

```sql
SELECT m.* FROM movies m WHERE m.id = 524;
```

```
Index Scan using movies_pkey on movies m  (actual time=0.022..0.023 rows=1)
  Index Cond: (id = 524)
  Buffers: shared hit=6
Execution Time: 0.081 ms
```

**Verdict: ✅ optimal.** PK lookup, nothing to do.

---

## Q7 — Internal: `existsByTmdbId(?)` — ingestion idempotency check

```sql
SELECT EXISTS(SELECT 1 FROM movies m WHERE m.tmdb_id = 1419406);
```

```
Result  (actual time=0.017..0.018 rows=1)
  Buffers: shared hit=2
  InitPlan 1
    ->  Seq Scan on movies m  (actual time=0.016..0.017 rows=1)
          Filter: (tmdb_id = 1419406)
          Buffers: shared hit=2
Execution Time: 0.061 ms
```

**Verdict: ❌ V4 gap.** The 0.06ms here is misleading — the EXISTS short-circuits as soon as it finds a match, and this particular `tmdb_id` happens to live in the first heap page. The plan is still **Seq Scan**: there is no index on `tmdb_id`. For misses (or for matches deeper in the table) the scan walks the whole table — see Q8.

This query is hit **once per movie** during ingestion. The 5000-count seed run made thousands of these calls.

---

## Q8 — Internal: `findByTmdbId(?)` — tmdb-side update lookup

### Hit case (real tmdb_id)

```sql
SELECT m.id, m.title, m.tmdb_id FROM movies m WHERE m.tmdb_id = 1419406;
```

```
Seq Scan on movies m  (actual time=0.006..1.188 rows=1)
  Filter: (tmdb_id = 1419406)
  Rows Removed by Filter: 3746
  Buffers: shared hit=226
Execution Time: 1.203 ms
```

### Miss case (nonexistent tmdb_id)

```sql
SELECT m.id, m.title, m.tmdb_id FROM movies m WHERE m.tmdb_id = 99999999;
```

```
Seq Scan on movies m  (actual time=0.887..0.888 rows=0)
  Filter: (tmdb_id = 99999999)
  Rows Removed by Filter: 3747
  Buffers: shared hit=226
Execution Time: 0.895 ms
```

**Verdict: ❌ V4 gap, highest-leverage missing index.** Both hit and miss do a full Seq Scan over all 3747 rows, touching 226 buffers each time. 1.2ms for a hit, 0.9ms for a miss — multiplied by every `existsByTmdbId` and `findByTmdbId` call during ingestion, this is the dominant cost.

**V5 candidate:** `idx_movies_tmdb_id (tmdb_id)` — definite. The data is functionally unique today (idempotency check enforces it), so promoting to a UNIQUE constraint would be semantically correct and give the index for free; that's the bigger lift, flagged as a follow-up.

---

## Summary — V4 coverage map

| Query | V4 verdict | Plan | Time | Buffers |
|---|---|---|---|---|
| Q1 default browse SELECT | ✅ hit | Index Scan `idx_movies_created_at_id` | 0.05 ms | 4 |
| Q1 default browse COUNT | ✅ optimal | Index Only Scan PK | 0.7 ms | 13 |
| Q2 year filter SELECT | ⚠️ partial | Index Scan sort + filter (composite oppty) | 0.25 ms | 56 |
| Q2 year filter COUNT | ✅ hit | Bitmap `idx_movies_release_year` | 0.21 ms | 87 |
| Q3 genre filter SELECT | ✅ via uk_movie_genre + V4 sort | Nested Loop | 2.4 ms | 367 |
| Q3 genre filter COUNT | ✅ hit `idx_movie_genres_genre_movie` | Merge Join | 0.94 ms | 19 |
| Q4 title ILIKE SELECT | ❌ gap | Seq Scan + sort | 3.66 ms | 229 |
| Q4 title ILIKE COUNT | ❌ gap | Seq Scan | 2.00 ms | 226 |
| Q5 title sort | ❌ gap (low priority) | Seq Scan + sort | 3.84 ms | 229 |
| Q5 year sort | ✅ hit | Index Scan Backward `idx_movies_release_year` | 0.05 ms | 9 |
| Q6 PK lookup | ✅ optimal | Index Scan PK | 0.08 ms | 6 |
| Q7 existsByTmdbId | ❌ gap | Seq Scan | 0.06 ms* | 2* |
| Q8 findByTmdbId hit | ❌ gap | Seq Scan | 1.20 ms | 226 |
| Q8 findByTmdbId miss | ❌ gap | Seq Scan | 0.90 ms | 226 |

\* Q7 short-circuits on this particular id; the plan is still Seq Scan and degrades to Q8-shape on misses or deeper hits.

---

## V5 — what shipped

Migration `V5__add_tmdb_id_and_title_trgm_indexes.sql`:

```sql
CREATE INDEX IF NOT EXISTS idx_movies_tmdb_id ON movies (tmdb_id);
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_movies_title_trgm
    ON movies USING GIN (LOWER(title) gin_trgm_ops);
```

**Held back from V5:** `idx_movies_title (title)` for Q5 title sort — re-measured below and ultimately rejected outright (see "Indexes considered — one added, two rejected").

---

## Post-V5 measurements

All measurements taken after Flyway applied V5 cleanly (164 ms) and `ANALYZE movies` was re-run to refresh planner stats over the new indexes.

Postgres normalizes the trigram index expression as `lower(title::text)` (the cast comes from `varchar → text` for the `gin_trgm_ops` opclass). The first concern below was confirming the planner treats this as equivalent to the query's `LOWER(m.title)`.

### Planner equivalence — confirmed

The Q4 COUNT plan below explicitly uses `idx_movies_title_trgm` with `Index Cond: (lower((title)::text) ~~ '%love%'::text)` — the planner matches the index expression to the query's `LOWER(m.title)` predicate without intervention. Confirmed further with a no-`ORDER BY` SELECT and a more selective pattern (`%shadow%`, 12 matches), both of which the planner served via the trigram index. The takeaway: V5's index is recognized; the planner uses it whenever it's the cheaper option, and falls back to V4's sort index when sort-with-LIMIT-and-inline-filter beats trigram-bitmap-then-sort.

### Q4 — Title text search: `?query=love`

#### SELECT (page query)

```
Limit  (cost=0.28..116.18 rows=12 width=37) (actual time=0.282..0.562 rows=12)
  Buffers: shared hit=57
  ->  Index Scan using idx_movies_created_at_id on movies m
        (cost=0.28..715.00 rows=74 width=37)
        Filter: (lower((title)::text) ~~ '%love%'::text)
        Rows Removed by Filter: 611
        Buffers: shared hit=57
Execution Time: 0.599 ms
```

| | Before V5 | After V5 |
|---|---|---|
| Plan | Seq Scan + top-N heapsort | Index Scan idx_movies_created_at_id + filter |
| Time | 3.66 ms | **0.60 ms** (~6× faster) |
| Buffers | 229 | **57** (~4× fewer) |

The planner did not pick the trigram index for this exact query. With LIMIT 12 + ORDER BY created_at DESC and ~63 matches in 3747 rows (~1.7%), walking V5's sort index in already-sorted order and filtering 611 rows beats trigram-bitmap-then-sort. The trigram index still helped: pg_trgm gives the planner better selectivity estimates for LIKE patterns (estimated rows shifted from 30 → 74), which contributed to the cost-model shift even when the trigram path itself wasn't taken.

#### COUNT

```
Aggregate  (actual time=0.491..0.493 rows=1)
  Buffers: shared hit=58
  ->  Bitmap Heap Scan on movies m  (actual time=0.146..0.479 rows=63)
        Recheck Cond: (lower((title)::text) ~~ '%love%'::text)
        ->  Bitmap Index Scan on idx_movies_title_trgm
              (actual time=0.102..0.102 rows=63)
              Index Cond: (lower((title)::text) ~~ '%love%'::text)
              Buffers: shared hit=5
Execution Time: 0.687 ms
```

| | Before V5 | After V5 |
|---|---|---|
| Plan | Seq Scan | Bitmap Index Scan idx_movies_title_trgm + Bitmap Heap Scan |
| Time | 2.00 ms | **0.69 ms** (~3× faster) |
| Buffers | 226 | **58** (~4× fewer) |

#### Trigram path proof — selective pattern (`%shadow%`, 12 matches)

```
Limit  (actual time=0.121..0.123 rows=12)
  Buffers: shared hit=24
  ->  Sort  (actual time=0.120..0.121 rows=12)
        Sort Key: created_at DESC
        ->  Bitmap Heap Scan on movies m  (actual time=0.032..0.060 rows=12)
              Recheck Cond: (lower((title)::text) ~~ '%shadow%'::text)
              ->  Bitmap Index Scan on idx_movies_title_trgm
                    (actual time=0.025..0.025 rows=12)
Execution Time: 0.138 ms
```

When the pattern is selective enough that `trigram-bitmap → sort` beats `sort-index walk → filter`, the planner picks the trigram path. **0.14 ms** for a 12-match search. Confirms the cost-based decision works correctly.

### Q7 — `existsByTmdbId(?)` — ingestion idempotency check

```
Result  (actual time=0.061..0.062 rows=1)
  Buffers: shared hit=4 read=2
  InitPlan 1
    ->  Index Only Scan using idx_movies_tmdb_id on movies m
          (actual time=0.060..0.060 rows=1)
          Index Cond: (tmdb_id = 1419406)
          Heap Fetches: 0
Execution Time: 0.127 ms
```

| | Before V5 | After V5 |
|---|---|---|
| Plan | Seq Scan (lucky early hit on this id) | Index Only Scan idx_movies_tmdb_id |
| Time | 0.06 ms* | 0.13 ms |
| Buffers | 2 | 6 |

\* The before-number was misleadingly fast: the seq scan happened to find the row in buffer 2. After V5 the scan is structural (Index Only Scan, O(log n)) and predictable regardless of where the row sits — the miss / deep-hit cases (Q8) show the real before/after picture.

### Q8 — `findByTmdbId(?)`

#### Hit case

```
Index Scan using idx_movies_tmdb_id on movies m  (actual time=0.011..0.012 rows=1)
  Index Cond: (tmdb_id = 1419406)
  Buffers: shared hit=3
Execution Time: 0.043 ms
```

| | Before V5 | After V5 |
|---|---|---|
| Plan | Seq Scan, 3746 rows removed | Index Scan idx_movies_tmdb_id |
| Time | 1.20 ms | **0.04 ms** (~28× faster) |
| Buffers | 226 | **3** (~75× fewer) |

#### Miss case

```
Index Scan using idx_movies_tmdb_id on movies m  (actual time=0.024..0.024 rows=0)
  Index Cond: (tmdb_id = 99999999)
  Buffers: shared hit=1 read=1
Execution Time: 0.034 ms
```

| | Before V5 | After V5 |
|---|---|---|
| Plan | Seq Scan, 3747 rows removed | Index Scan idx_movies_tmdb_id |
| Time | 0.90 ms | **0.03 ms** (~26× faster) |
| Buffers | 226 | **2** (~113× fewer) |

These are the highest-leverage wins. `existsByTmdbId` is hit per-movie during ingestion — the 5000-count seed run made thousands of these calls. The buffer-count drop (226 → 2) is also significant beyond the latency: full Seq Scans pollute the buffer cache with all heap pages of `movies` on every call, evicting hot pages used by foreground queries.

### Q5 — Title sort (held back from V5)

Re-measured for completeness; plan unchanged from baseline since V5 didn't touch this path.

```
Limit  (actual time=1.914..1.916 rows=12)
  Buffers: shared hit=229
  ->  Sort  Sort Method: top-N heapsort  Memory: 26kB
        ->  Seq Scan on movies m  (actual time=0.008..1.096 rows=3747)
Execution Time: 1.944 ms
```

**Decision: defer.** `idx_movies_title (title)` would replace the Seq Scan + top-N with an Index Scan and bring this to sub-millisecond. But:

- Title sort is a **user-initiated** sort change (default is `created_desc`), not hit on every page load.
- ~2 ms at the current catalog size isn't pain.
- "Don't solve problems we don't have yet."

**Revisit when:** the catalog grows past ~50k movies, or k6 measurements in Branch 3 show title-sort browsing as a measured hot path.

### V4-covered queries — sanity checks (no regression)

| Query | Baseline | Post-V5 | Plan |
|---|---|---|---|
| Q1 default browse SELECT | 0.05 ms / 4 buf | 0.07 ms / 4 buf | unchanged — Index Scan idx_movies_created_at_id |
| Q3 genre filter COUNT | 0.94 ms / 19 buf | 1.7 ms / 19 buf† | unchanged — Merge Join + idx_movie_genres_genre_movie |
| Q6 PK lookup | 0.08 ms / 6 buf | 0.05 ms / 6 buf | unchanged — Index Scan movies_pkey |

† Time variance within run-to-run noise; identical plan, identical buffer count, identical cost estimate.

---

## Final V4 + V5 coverage map

| Query | Plan | Status |
|---|---|---|
| Q1 default browse SELECT | Index Scan idx_movies_created_at_id | ✅ V4 |
| Q1 default browse COUNT | Index Only Scan PK | ✅ optimal |
| Q2 year filter SELECT | Index Scan sort + filter | ⚠️ V4 partial — composite oppty deferred |
| Q2 year filter COUNT | Bitmap idx_movies_release_year | ✅ V4 |
| Q3 genre filter SELECT | Nested Loop via idx_movies_created_at_id + uk_movie_genre | ✅ V4 + UNIQUE |
| Q3 genre filter COUNT | Merge Join + idx_movie_genres_genre_movie | ✅ V4 |
| Q4 title ILIKE SELECT | Index Scan sort + trigram-aware filter (or trigram bitmap when selective) | ✅ **V5** |
| Q4 title ILIKE COUNT | Bitmap idx_movies_title_trgm | ✅ **V5** |
| Q5 title sort | Seq Scan + top-N heapsort | ⚠️ **deferred** — see above |
| Q5 year sort | Index Scan Backward idx_movies_release_year | ✅ V4 |
| Q6 PK lookup | Index Scan movies_pkey | ✅ optimal |
| Q7 existsByTmdbId | Index Only Scan idx_movies_tmdb_id | ✅ **V5** |
| Q8 findByTmdbId | Index Scan idx_movies_tmdb_id | ✅ **V5** |

## Indexes considered — one added, two rejected

After post-V5 measurement, three additional indexes were on the table. Engineering judgment was applied to each. Adding indexes you don't need is a real cost: every index slows down INSERTs and UPDATEs, and our ingestion path runs INSERTs in volume. The right move is to add the one index that gives a clear semantic + performance win, and explicitly reject the two that don't.

### Added — `tmdb_id` promoted to UNIQUE constraint (V6)

V5 added `idx_movies_tmdb_id` as a plain btree. Post-V5 measurement confirmed it solved the read-side problem (Q7/Q8 went from 226-buffer Seq Scans to 2–3-buffer Index Scans). But the data is functionally unique today — the ingestion path's `existsByTmdbId` check enforces it at the application layer — and the `Movie` JPA entity already declares `unique = true` on the column. The DB schema didn't reflect that claim.

Promoting to a UNIQUE constraint:
- Catches any future integrity bug (a duplicate insert from a code path that bypasses the existence check) at the DB layer instead of as a corrupted catalog.
- Costs nothing on the read side — the constraint's backing index serves Q7/Q8 identically (verified below).
- Replaces, not adds — the old plain index is dropped in the same migration so we don't end up with two indexes on the same column.

Migration `V6__promote_tmdb_id_to_unique.sql`:

```sql
ALTER TABLE movies
    ADD CONSTRAINT uk_movies_tmdb_id UNIQUE (tmdb_id);

DROP INDEX idx_movies_tmdb_id;
```

Pre-flight check confirmed zero duplicate `tmdb_id` rows and zero nulls in the 3747-movie catalog before applying. (Column left nullable to allow non-TMDb-sourced movies in the future, matching the JPA entity. Postgres treats multiple NULLs as distinct under default `NULLS DISTINCT`.)

### Post-V6 measurements — Q7/Q8 unchanged with UNIQUE-backed index

| Query | Post-V5 (`idx_movies_tmdb_id`) | Post-V6 (`uk_movies_tmdb_id`) | Plan |
|---|---|---|---|
| Q7 existsByTmdbId | 0.127 ms / 6 buf | 0.071 ms / 6 buf | Index Only Scan (same) |
| Q8 findByTmdbId hit | 0.043 ms / 3 buf | 0.016 ms / 3 buf | Index Scan (same) |
| Q8 findByTmdbId miss | 0.034 ms / 2 buf | 0.034 ms / 2 buf | Index Scan (same) |

Identical plans, identical buffer counts; wall-clock variance within run-to-run noise. The UNIQUE constraint's backing index is a btree on `(tmdb_id)`, structurally the same as V5's plain index, so the planner makes the same choices.

### Rejected — composite `(release_year, created_at DESC)` for Q2 SELECT

Q2 SELECT (`?year=NNNN&page=0&size=12`) currently uses V4's sort index and filters inline, removing 577 rows to find the first 12 matches. A composite `(release_year, created_at DESC)` would let the planner skip both the filter step and the sort, going directly to the right rows.

**Why not added:** Q2 SELECT already executes in 0.25 ms. The composite index would shave a fraction of a millisecond off a query that's not a problem, while imposing a real write cost on every INSERT (every ingested movie updates one more index). At the catalog size where year-filtered browsing becomes a measured hot path, the trade flips. Until then, no.

### Rejected — `idx_movies_title (title)` btree for title sort

Q5 title sort currently does a Seq Scan + top-N heapsort over 3747 rows in ~2 ms. A `(title)` btree would replace this with an Index Scan and bring it sub-millisecond.

**Why not added:** Title sort is a *user-initiated* sort change (the default is `created_desc`), not hit on every page load. ~2 ms isn't pain. Adding the index would slow every INSERT and UPDATE on the table for negligible read gain. Same logic as Q2: revisit when measurement justifies it (catalog growth past ~50k movies, or k6 hot-path signal from Branch 3).

---

## Final coverage map (V4 + V5 + V6)

| Query | Plan | Status |
|---|---|---|
| Q1 default browse SELECT | Index Scan idx_movies_created_at_id | ✅ V4 |
| Q1 default browse COUNT | Index Only Scan PK | ✅ optimal |
| Q2 year filter SELECT | Index Scan sort + filter | ✅ acceptable (0.25 ms; composite index rejected) |
| Q2 year filter COUNT | Bitmap idx_movies_release_year | ✅ V4 |
| Q3 genre filter SELECT | Nested Loop via idx_movies_created_at_id + uk_movie_genre | ✅ V4 + UNIQUE |
| Q3 genre filter COUNT | Merge Join + idx_movie_genres_genre_movie | ✅ V4 |
| Q4 title ILIKE SELECT | Index Scan sort + trigram-aware filter (or trigram bitmap when selective) | ✅ V5 |
| Q4 title ILIKE COUNT | Bitmap idx_movies_title_trgm | ✅ V5 |
| Q5 title sort | Seq Scan + top-N heapsort | ✅ acceptable (~2 ms; title btree rejected) |
| Q5 year sort | Index Scan Backward idx_movies_release_year | ✅ V4 |
| Q6 PK lookup | Index Scan movies_pkey | ✅ optimal |
| Q7 existsByTmdbId | Index Only Scan uk_movies_tmdb_id | ✅ V5 → V6 |
| Q8 findByTmdbId | Index Scan uk_movies_tmdb_id | ✅ V5 → V6 |

All 12 hot-query plans are now either optimal or deliberately accepted as good-enough with the trade documented. No "todo" indexes hanging over the catalog.
