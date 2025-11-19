CREATE TABLE IF NOT EXISTS ratings (
    id               BIGSERIAL PRIMARY KEY,
    user_id          UUID           NOT NULL,
    movie_id         BIGINT         NOT NULL,
    rating_times_ten  INT            NOT NULL,     -- e.g. 81 = 8.1/10
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL
);

-- Enforce one rating per (user, movie) pair
ALTER TABLE ratings
    ADD CONSTRAINT uk_ratings_user_movie UNIQUE (user_id, movie_id);

-- Helpful indexes for common queries
CREATE INDEX IF NOT EXISTS idx_ratings_movie_id ON ratings(movie_id);  -- list ratings for a movie
CREATE INDEX IF NOT EXISTS idx_ratings_user_id  ON ratings(user_id);   -- list ratings by a user
