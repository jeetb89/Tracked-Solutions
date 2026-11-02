#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="$ROOT_DIR/order-management-service"

for command in java mvn docker; do
  if ! command -v "$command" >/dev/null 2>&1; then
    printf 'Required command not found: %s\n' "$command" >&2
    exit 1
  fi
done

if ! docker compose version >/dev/null 2>&1; then
  printf 'Docker Compose is unavailable. Start Docker Desktop and install Docker Compose.\n' >&2
  exit 1
fi

printf 'Starting PostgreSQL and Redis...\n'
docker compose -f "$ROOT_DIR/compose.yaml" up -d --wait postgres redis

printf 'Starting the Order Management Service at http://localhost:8080...\n'
cd "$SERVICE_DIR"
exec env \
  SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-app}}" \
  SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-${POSTGRES_USER:-app}}" \
  SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-${POSTGRES_PASSWORD:-localdev}}" \
  REDIS_HOST="${REDIS_HOST:-localhost}" \
  REDIS_PORT="${REDIS_PORT:-6379}" \
  mvn spring-boot:run
