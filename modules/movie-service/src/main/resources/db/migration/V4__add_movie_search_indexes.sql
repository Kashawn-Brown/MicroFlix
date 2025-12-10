-- Index for default sort: newest movies first
-- created_at DESC, id ASC so we have a stable order
CREATE INDEX IF NOT EXISTS idx_movies_created_at_id
    ON movies (created_at DESC, id);

-- Index for filtering by release year
CREATE INDEX IF NOT EXISTS idx_movies_release_year
    ON movies (release_year);

-- Index for filtering by genre via join table
-- Adjust table/column names if yours differ
CREATE INDEX IF NOT EXISTS idx_movie_genres_genre_movie
    ON movie_genres (genre_id, movie_id);
