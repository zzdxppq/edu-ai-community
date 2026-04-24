# Database Cumulative Registry

> Initialized on Story 1.2 completion.
> Updated by Dev Agent after each story completion.

## Registry Metadata

**Last Updated**: 2026-04-24
**Total Stories Tracked**: 2 (1.1, 1.2)
**Repository**: edu-ai-community
**Mode**: monolith

## Database Tables Registry

### `users` (edu-core)

- **Created in**: Story 1.2
- **Status**: active
- **Migration**: `backend/edu-core/src/main/resources/db/migration/V1__create_users.sql`

| Field | Type | Constraints | Added in |
|-------|------|-------------|----------|
| id | uuid | primary key, default gen_random_uuid() | 1.2 |
| phone | varchar(11) | not null, unique | 1.2 |
| username | varchar(20) | not null | 1.2 |
| role | varchar(30) | not null | 1.2 |
| user_type | varchar(10) | not null, default 'USER' | 1.2 |
| region | varchar(100) | nullable | 1.2 |
| school | varchar(100) | nullable | 1.2 |
| password_hash | varchar(255) | nullable (预留) | 1.2 |
| created_at | timestamptz | not null, default now() | 1.2 |
| updated_at | timestamptz | not null, default now() | 1.2 |
| last_login_at | timestamptz | nullable | 1.2 |

**Indexes**:
- `idx_users_phone` — btree on phone
- `idx_users_role` — btree on role

### PostgreSQL Extensions (edu-ai)

- `vector` (pgvector) — Story 1.1, migration `V1__init_extensions.sql`
- `zhparser` (optional, image-dependent) — Story 1.1

## Naming Conventions & Patterns

- Table naming: snake_case, plural (e.g., `users`)
- Field naming: snake_case
- Primary key: `id` (uuid, `gen_random_uuid()`)
- Timestamps: `created_at`, `updated_at`, `last_login_at`
- Enum-typed columns: `VARCHAR(N)` storing enum name (not native PG ENUM type)

## Schema Evolution Timeline

- **Story 1.1** (2026-04-22): Bootstrapped `edu-ai` DB with `vector` + `zhparser` extensions. No tables created (`edu-core/db/migration` directory deferred to 1.2).
- **Story 1.2** (2026-04-24): First `edu-core` migration `V1__create_users.sql` — `users` table + 2 indexes. 11 fields. Separate Flyway schema history from `edu-ai`.
