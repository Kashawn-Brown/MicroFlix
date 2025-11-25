-- V2__create_engagements.sql
-- Engagements table to store watchlist/favorite/like relationships
-- between a user and a movie.

CREATE TABLE IF NOT EXISTS engagements (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID            NOT NULL,
    movie_id    BIGINT          NOT NULL,
    type        VARCHAR(32)     NOT NULL,   -- e.g., WATCHLIST, FAVORITE, LIKE
    created_at  TIMESTAMPTZ     NOT NULL,

    CONSTRAINT uk_engagement_user_movie_type
        UNIQUE (user_id, movie_id, type)
        -- E.g. user can only have one WATCHLIST engagement per movie
);
