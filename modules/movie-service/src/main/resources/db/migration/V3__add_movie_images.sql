-- V3__add_movie_images.sql
-- Add poster and backdrop URLs to movies table

ALTER TABLE movies
    ADD COLUMN IF NOT EXISTS poster_url   TEXT,
    ADD COLUMN IF NOT EXISTS backdrop_url TEXT;
