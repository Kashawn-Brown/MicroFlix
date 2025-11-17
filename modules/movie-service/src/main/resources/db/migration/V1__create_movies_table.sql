CREATE TABLE IF NOT EXISTS movies (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255)            NOT NULL,
    overview        TEXT,
    release_year    INT,
    runtime         INT,
    tmdb_id         BIGINT,                  --NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ             NOT NULL,
    updated_at      TIMESTAMPTZ             NOT NULL
);
