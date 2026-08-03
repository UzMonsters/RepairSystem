# RepairAuto Backend

RepairAuto is a repair-service CRM backend. The backend is the source of truth
for workflow rules, permissions, validation, persistence, and side effects.

This repository is currently in Phase 12: Production Hardening and Release
Readiness. Authentication, user management, core reference data, request intake,
assignment and scheduling, execution lifecycle, attachments, customer and
technician Telegram workflows, reliable notification outbox, reviews, dashboard
analytics, and production hardening are implemented. Pricing, payments,
spare-parts inventory, and business audit logs are intentionally not
implemented.

## Stack

- Java 21
- Gradle
- Spring Boot 4.1.0, preserved from the generated repository
- PostgreSQL
- MinIO for local S3-compatible object storage
- Flyway
- Spring Security
- Spring Boot Actuator
- Springdoc OpenAPI
- JUnit 5, Mockito, Testcontainers
- JaCoCo coverage reporting
- Checkstyle import hygiene checks

## Local Configuration

Copy `.env.example` to `.env` for Docker Compose usage and override secrets
locally. Do not commit real secrets.

Key environment variables:

- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_OPENAPI_ENABLED`
- `APP_SWAGGER_UI_ENABLED`
- `APP_JWT_SECRET`
- `APP_JWT_ISSUER`
- `APP_JWT_ACCESS_TOKEN_TTL`
- `APP_REFRESH_TOKEN_TTL`
- `APP_BOOTSTRAP_ADMIN_ENABLED`
- `APP_BOOTSTRAP_ADMIN_EMAIL`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `APP_BOOTSTRAP_ADMIN_FULL_NAME`
- `APP_STORAGE_ENABLED`
- `APP_STORAGE_ENDPOINT`
- `APP_STORAGE_REGION`
- `APP_STORAGE_BUCKET`
- `APP_STORAGE_ACCESS_KEY`
- `APP_STORAGE_SECRET_KEY`
- `APP_STORAGE_PATH_STYLE`
- `APP_STORAGE_CREATE_BUCKET`
- `APP_STORAGE_DOWNLOAD_URL_TTL`
- `APP_STORAGE_MAX_FILE_SIZE`
- `APP_STORAGE_MAX_FILES_PER_REQUEST`
- `APP_STORAGE_MAX_FILES_PER_TYPE`
- `APP_AUTH_THROTTLE_ENABLED`
- `APP_AUTH_THROTTLE_MAX_FAILURES`
- `APP_AUTH_THROTTLE_WINDOW`
- `APP_AUTH_THROTTLE_BLOCK_DURATION`
- `APP_AUTH_THROTTLE_RETENTION`
- `APP_CLEANUP_ENABLED`
- `APP_CLEANUP_INTERVAL`
- `APP_DASHBOARD_BUSINESS_TIME_ZONE`

`APP_JWT_SECRET` must contain at least 32 characters. The bootstrap admin is
disabled by default. Enable it only for initial setup, provide a policy-compliant
password, log in once, then disable bootstrap again.

## Run Locally

With Docker available:

```bash
docker compose up --build
```

Swagger and OpenAPI are disabled by Docker Compose defaults. Enable them
explicitly for local development:

```bash
APP_OPENAPI_ENABLED=true APP_SWAGGER_UI_ENABLED=true docker compose up --build
```

Useful endpoints in local development:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /v3/api-docs`
- `GET /swagger-ui.html`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`
- `GET /api/v1/users`
- `GET /api/v1/customers`
- `GET /api/v1/technicians`
- `GET /api/v1/categories`
- `GET /api/v1/requests`
- `POST /api/v1/requests`
- `GET /api/v1/customers/{customerId}/requests`
- `POST /api/v1/requests/{requestId}/assign`
- `POST /api/v1/requests/{requestId}/reassign`
- `PATCH /api/v1/requests/{requestId}/schedule`
- `GET /api/v1/requests/{requestId}/assignments`
- `GET /api/v1/technicians/{technicianId}/workload`
- `POST /api/v1/requests/{requestId}/start`
- `PATCH /api/v1/requests/{requestId}/diagnosis`
- `POST /api/v1/requests/{requestId}/wait-for-parts`
- `POST /api/v1/requests/{requestId}/resume`
- `POST /api/v1/requests/{requestId}/complete`
- `POST /api/v1/requests/{requestId}/cancel`
- `GET /api/v1/requests/{requestId}/execution`
- `GET /api/v1/requests/{requestId}/status-history`
- `POST /api/v1/requests/{requestId}/attachments`
- `GET /api/v1/requests/{requestId}/attachments`
- `GET /api/v1/attachments/{attachmentId}`
- `GET /api/v1/attachments/{attachmentId}/download-url`
- `DELETE /api/v1/attachments/{attachmentId}`

Protected API requests use `Authorization: Bearer <accessToken>`. Refresh
tokens are opaque one-time tokens and are stored only as SHA-256 hashes.
Access tokens carry `authVersion`; password changes, role changes,
deactivation, logout-all, and admin session revocation atomically increment the
database value to invalidate older access tokens immediately.

User listing supports only whitelisted public sort fields: `id`, `fullName`,
`email`, `role`, `active`, `createdAt`, `updatedAt`, and `lastLoginAt`.
Invalid page, size, sort field, direction, or malformed sort expressions return
`400 INVALID_REQUEST_PARAMETER`. Page size is bounded to `1..100`.

Phase 2 list endpoints use the same pagination bounds and explicit sort
whitelists. Customers and technicians are manageable by `ADMIN` and `MANAGER`.
Repair categories are readable by both roles, while create, update, archive, and
reorder operations are `ADMIN` only.

Phase 3 request-intake endpoints require `ADMIN` or `MANAGER`. New REST-created
requests always use backend-controlled `status=NEW`, `source=ADMIN`, and the
authenticated user as creator. Public request numbers use a PostgreSQL sequence
and UTC-year format such as `REP-2026-000001`; the sequence is global and does
not reset annually. See `docs/REPAIR_REQUESTS.md`.

Phase 4 assignment endpoints also require `ADMIN` or `MANAGER`. Assignment can
move requests among `NEW`, `ASSIGNED`, and `SCHEDULED`.

Phase 5 lifecycle endpoints require `ADMIN` or `MANAGER`. They move requests
through `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`, and `CANCELLED`, record
append-only status history, and release technician workload capacity when a
repair is completed or cancelled.

Phase 6 attachment endpoints require `ADMIN` or `MANAGER`. Files are streamed to
private S3-compatible storage, metadata is stored in PostgreSQL, and downloads
use short-lived authorized URLs. Completing a repair now requires at least one
available `COMPLETION_PHOTO`. See `docs/ATTACHMENTS.md`.

Phase 12 production hardening enables strict `prod` profile startup validation,
DB-backed auth throttling, scheduled technical-data cleanup, security headers,
notification operational metrics, non-root container runtime, and release-gate
documentation. Start with `docs/DEPLOYMENT.md`, `docs/SECURITY.md`, and
`docs/operations/RELEASE_CHECKLIST.md` for production rollout.

Uzbekistan MVP phone numbers are normalized to `+998XXXXXXXXX`. Accepted forms
include `+998 90 123 45 67`, `998901234567`, and `90 123 45 67`.

The core backend language set is exactly `EN`, `RU`, and `UZ`. Customer and
technician `preferredLanguage` use the shared enum, and repair categories store
English, Russian, and Uzbek names and optional descriptions.

## Verification

The CI-ready verification command is:

```bash
./gradlew clean check bootJar
```

PostgreSQL integration tests use Testcontainers and require Docker.

If this project is initialized as a Git repository on Linux/macOS CI, ensure the
wrapper executable bit is preserved:

```bash
git update-index --chmod=+x gradlew
```

Coverage report:

```text
build/reports/jacoco/test/html/index.html
```
