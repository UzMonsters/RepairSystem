# Deployment

## Profiles

- `local`: developer defaults.
- `test`: integration-test defaults.
- `prod`: no usable secret defaults and strict startup validation.

## Production Requirements

- PostgreSQL 17-compatible database.
- Private S3-compatible object storage.
- HTTPS reverse proxy at the edge.
- Environment-based secret injection.
- Persistent PostgreSQL backups.
- Log aggregation and metrics scraping on a protected network.

## Startup

1. Apply production environment variables.
2. Start PostgreSQL and object storage.
3. Start backend with `SPRING_PROFILES_ACTIVE=prod`.
4. Verify `/actuator/health/readiness`.
5. Register Telegram webhook with `scripts/telegram-webhook.ps1` when Telegram is enabled.

## Rollback

Application rollback is safe only to a version that supports the already-applied database schema. Flyway migrations are forward-only; database rollback requires restore from backup or a tested corrective migration.

## Docker

Use `docker-compose.prod.example.yml` as a production-oriented example. It keeps PostgreSQL and MinIO on a private network, uses restart policies, log rotation, resource limits, a non-root backend runtime, and only exposes the backend port.
