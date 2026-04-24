-- Story 1.2 T0: users table (edu-core schema).
-- Source: docs/fullstack-architecture/database-schema.md §7.2
-- gen_random_uuid() is provided by PG 16 built-in (no pgcrypto extension required).

CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(11)  NOT NULL UNIQUE,
    username        VARCHAR(20)  NOT NULL,
    role            VARCHAR(30)  NOT NULL,
    user_type       VARCHAR(10)  NOT NULL DEFAULT 'USER',
    region          VARCHAR(100),
    school          VARCHAR(100),
    password_hash   VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMPTZ
);

CREATE INDEX idx_users_phone ON users (phone);
CREATE INDEX idx_users_role  ON users (role);
