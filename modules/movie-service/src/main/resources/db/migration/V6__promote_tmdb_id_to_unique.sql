-- Promote tmdb_id from a plain index to a UNIQUE constraint.
-- The data is functionally unique today (ingestion's existsByTmdbId guard
-- enforces it at the application layer); moving the guarantee into the DB
-- catches integrity bugs at the right layer. The Movie JPA entity already
-- declares unique = true on this column — the schema now matches that claim.
-- The constraint's backing index serves existsByTmdbId / findByTmdbId
-- identically to V5's plain btree, so V5's index is dropped here.

ALTER TABLE movies
    ADD CONSTRAINT uk_movies_tmdb_id UNIQUE (tmdb_id);

DROP INDEX idx_movies_tmdb_id;
