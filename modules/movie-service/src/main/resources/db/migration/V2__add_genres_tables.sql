-- V2__add_genres_tables.sql
-- Normalized genres + movie_genres join table

-- Movie ↔ MovieGenre ↔ Genre

-- table for all genres
CREATE TABLE IF NOT EXISTS genres (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE
);

-- table connecting movies to their genres (each row: links one movie to one genre)
CREATE TABLE IF NOT EXISTS movie_genres (
    id        BIGSERIAL PRIMARY KEY,
    movie_id  BIGINT      NOT NULL,         -- FK referencing movies.id
    genre_id  BIGINT      NOT NULL,         -- FK referencing genres.id

    CONSTRAINT fk_movie_genres_movie        -- connecting to movies table
        FOREIGN KEY (movie_id)
            REFERENCES movies (id)
            ON DELETE CASCADE,              -- If a movie is deleted, its rows in movie_genres are deleted

    CONSTRAINT fk_movie_genres_genre        -- connecting to genres table
        FOREIGN KEY (genre_id)
            REFERENCES genres (id)
            ON DELETE CASCADE,              -- If a genre is deleted (rare), its rows in movie_genres are also deleted

    CONSTRAINT uk_movie_genre
        UNIQUE (movie_id, genre_id)        -- prevent duplicate links (can’t link the same movie to the same genre more than once)
);
