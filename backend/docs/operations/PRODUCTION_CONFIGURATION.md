# Production Configuration

RepairAuto stores technical timestamps as UTC instants and uses `Asia/Tashkent` for business and customer-facing time.

## Required Environment Variables

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`
- `APP_BUSINESS_TIME_ZONE=Asia/Tashkent`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_BOOTSTRAP_ADMIN_ENABLED`
- `APP_STORAGE_ENABLED`
- `APP_STORAGE_ENDPOINT`
- `APP_STORAGE_REGION`
- `APP_STORAGE_BUCKET`
- `APP_STORAGE_ACCESS_KEY`
- `APP_STORAGE_SECRET_KEY`
- `APP_TELEGRAM_ENABLED`
- `APP_TELEGRAM_CUSTOMER_BOT_TOKEN` when Telegram is enabled
- `APP_TELEGRAM_CUSTOMER_WEBHOOK_SECRET` when Telegram is enabled
- `APP_TELEGRAM_CUSTOMER_BOT_USERNAME` when Telegram is enabled
- `APP_TELEGRAM_TECHNICIAN_BOT_TOKEN` when Telegram is enabled
- `APP_TELEGRAM_TECHNICIAN_WEBHOOK_SECRET` when Telegram is enabled
- `APP_TELEGRAM_TECHNICIAN_BOT_USERNAME` when Telegram is enabled

Legacy dashboard grouping may still read `APP_DASHBOARD_BUSINESS_TIME_ZONE`; set it to `Asia/Tashkent` or leave the application default aligned with `APP_BUSINESS_TIME_ZONE`.

## Timezone Strategy

- JVM default timezone: force UTC with `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC`.
- Jackson serialization timezone: UTC.
- Hibernate JDBC timezone: UTC.
- Database timestamp columns: `timestamp with time zone`; no migration is currently needed.
- Business timezone: `Asia/Tashkent`.

Do not store formatted Tashkent date strings in the database. Store instants and format only at the edge.

## Display Expectations

API timestamp fields should continue to use instant-aware types such as `OffsetDateTime` and return ISO-8601 timestamps. The frontend should render customer-facing schedules in Tashkent time.

Telegram uses separate customer and technician bots. Register the customer bot webhook at `/api/v1/telegram/webhook/customer` and the technician bot webhook at `/api/v1/telegram/webhook/technician`. Customer notifications and photo downloads use the customer bot token; technician assignment messages, workflow callbacks, and invite links use the technician bot token.

Telegram notification dates are formatted in `Asia/Tashkent` as `dd.MM.yyyy HH:mm` for scheduled visits and reschedules.

## Swagger And CORS

Swagger/OpenAPI is disabled by default in production:

- `APP_OPENAPI_ENABLED=false`
- `APP_SWAGGER_UI_ENABLED=false`

Production CORS must use explicit HTTPS origins through `APP_CORS_ALLOWED_ORIGINS`; wildcard `*` is rejected by startup validation.

## PostgreSQL And MinIO

PostgreSQL credentials must come from environment variables:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

MinIO credentials and application storage settings must come from environment variables:

- `MINIO_ROOT_USER`
- `MINIO_ROOT_PASSWORD`
- `APP_STORAGE_ENDPOINT`
- `APP_STORAGE_BUCKET`
- `APP_STORAGE_ACCESS_KEY`
- `APP_STORAGE_SECRET_KEY`

## Starting Production

With required variables exported, start the production example stack:

```powershell
docker compose -f docker-compose.prod.example.yml up -d
```

For a direct JVM start, keep the same environment variables and include:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:APP_BUSINESS_TIME_ZONE = "Asia/Tashkent"
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=UTC"
.\gradlew bootRun
```
