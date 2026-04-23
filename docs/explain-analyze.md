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

## V5 — proposed contents (subject to post-V5 measurement)

Three indexes, in priority order:

1. **`idx_movies_tmdb_id (tmdb_id)`** — definite. Fixes Q7 and Q8 (highest-leverage; hit thousands of times per ingestion run).
2. **`pg_trgm` extension + `idx_movies_title_trgm` GIN on `lower(title) gin_trgm_ops`** — for Q4. Verified `pg_trgm 1.6` available in this Postgres image.
3. **`idx_movies_title (title)`** — for Q5 title sort. Borderline at 3747 rows but cheap; include only if Step-4 re-measurement shows the planner picks it up usefully.

**Explicitly deferred:**
- Composite `(release_year, created_at DESC)` for Q2 SELECT — current plan is already sub-millisecond; revisit only if year-filtered browsing becomes a measured hot path.
- Promoting `tmdb_id` to a UNIQUE constraint — semantically correct, gives the index for free, but bigger lift and orthogonal to the perf goal. Flagged as a follow-up.
