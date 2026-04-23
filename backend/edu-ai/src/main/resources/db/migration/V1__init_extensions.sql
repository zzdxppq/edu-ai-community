-- Story 1.1 — Flyway baseline for edu-ai.
-- Enables PostgreSQL extensions required by later RAG / full-text search stories.
--
-- Production note: CREATE EXTENSION requires SUPERUSER privileges. In local
-- development the pgvector/pgvector:pg16 image runs as the `postgres` superuser,
-- so this migration executes cleanly. In production, a DBA pre-provisions these
-- extensions; the IF NOT EXISTS clauses keep this migration idempotent.

CREATE EXTENSION IF NOT EXISTS vector;

-- zhparser may not be present in all PG images (notably the default pgvector
-- image). Wrapping the statement in an anonymous block allows the migration
-- to succeed in environments without zhparser while still enabling it where
-- available. Later RAG stories will assert availability at startup.
DO $$
BEGIN
    BEGIN
        CREATE EXTENSION IF NOT EXISTS zhparser;
    EXCEPTION
        WHEN undefined_file THEN
            RAISE NOTICE 'zhparser extension is not available; skipping';
    END;

    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'zhparser')
       AND NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'zhcfg') THEN
        EXECUTE 'CREATE TEXT SEARCH CONFIGURATION zhcfg (PARSER = zhparser)';
    END IF;
END;
$$;
