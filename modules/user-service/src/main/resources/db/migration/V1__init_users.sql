CREATE TABLE IF NOT EXISTS users (
  id             UUID PRIMARY KEY,
  email          VARCHAR(255) NOT NULL UNIQUE,
  password_hash  VARCHAR(72)  NOT NULL,
  display_name   VARCHAR(100),
  roles          VARCHAR(100) NOT NULL DEFAULT 'USER',
  is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  last_login_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);     -- fast lookup to improve read queries
