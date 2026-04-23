#!/usr/bin/env bash
# One-shot local dev environment bootstrap for edu-ai-community Story 1.1.
# Starts: PostgreSQL 16 + pgvector (container), Redis 7 (container).
# After this script: run `mvn -pl backend -am spring-boot:run -pl backend/edu-gateway`
# (or similar) for backend modules, and `npm run dev` inside frontend/.

set -euo pipefail

PG_CONTAINER=edu-ai-pg
REDIS_CONTAINER=edu-ai-redis
PG_PASSWORD=${PG_PASSWORD:-postgres}
PG_DB=${PG_DB:-edu_ai}
PG_PORT=${PG_PORT:-5432}
REDIS_PORT=${REDIS_PORT:-6379}

echo "[dev-up] Ensuring PostgreSQL 16 + pgvector container..."
if ! docker ps -a --format '{{.Names}}' | grep -q "^${PG_CONTAINER}$"; then
  docker run -d \
    --name "${PG_CONTAINER}" \
    -e POSTGRES_PASSWORD="${PG_PASSWORD}" \
    -e POSTGRES_DB="${PG_DB}" \
    -p "${PG_PORT}:5432" \
    pgvector/pgvector:pg16
else
  docker start "${PG_CONTAINER}" >/dev/null
fi

echo "[dev-up] Ensuring Redis 7 container..."
if ! docker ps -a --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
  docker run -d \
    --name "${REDIS_CONTAINER}" \
    -p "${REDIS_PORT}:6379" \
    redis:7-alpine
else
  docker start "${REDIS_CONTAINER}" >/dev/null
fi

echo "[dev-up] Waiting for PostgreSQL to accept connections..."
for i in {1..30}; do
  if docker exec "${PG_CONTAINER}" pg_isready -U postgres >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

echo "[dev-up] Enabling extensions (vector + zhparser if available)..."
# pgvector image ships `vector`; `zhparser` requires a custom image in production.
# For local dev this block is best-effort — Flyway (edu-ai V1) is the source of truth.
docker exec -i "${PG_CONTAINER}" psql -U postgres -d "${PG_DB}" <<'SQL'
CREATE EXTENSION IF NOT EXISTS vector;
-- zhparser is optional in local dev; enable only if the image has it.
DO $$
BEGIN
  BEGIN
    CREATE EXTENSION IF NOT EXISTS zhparser;
    IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'zhcfg') THEN
      CREATE TEXT SEARCH CONFIGURATION zhcfg (PARSER = zhparser);
    END IF;
  EXCEPTION WHEN undefined_file THEN
    RAISE NOTICE 'zhparser not available in this image; skipped.';
  END;
END;
$$;
SQL

echo "[dev-up] Pinging Redis..."
docker exec "${REDIS_CONTAINER}" redis-cli PING

cat <<EOM
[dev-up] Ready.
  PostgreSQL : localhost:${PG_PORT} (db=${PG_DB}, user=postgres, pwd=${PG_PASSWORD})
  Redis      : localhost:${REDIS_PORT}

Next steps:
  Backend : (cd backend && mvn -pl edu-gateway -am spring-boot:run)
            (cd backend && mvn -pl edu-core    -am spring-boot:run)
            (cd backend && mvn -pl edu-ai      -am spring-boot:run)
  Frontend: (cd frontend && npm install && npm run dev)
EOM
