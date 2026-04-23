-- Closes V4 gaps surfaced by EXPLAIN ANALYZE in docs/explain-analyze.md.

-- Index for tmdb_id lookups (existsByTmdbId, findByTmdbId).
-- Hit per-movie during ingestion; without this the planner does a Seq Scan.
CREATE INDEX IF NOT EXISTS idx_movies_tmdb_id
    ON movies (tmdb_id);

-- Trigram support for substring search on title.
-- LOWER(title) LIKE '%...%' has a leading % that defeats normal btree;
-- a pg_trgm GIN index over LOWER(title) lets the planner use trigram matching.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_movies_title_trgm
    ON movies USING GIN (LOWER(title) gin_trgm_ops);
