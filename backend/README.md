# RepairAuto Backend

RepairAuto is a repair-service CRM backend. The backend is the source of truth
for workflow rules, permissions, validation, persistence, and side effects.

This repository is currently in Phase 1: Authentication and User Management.
Customers, technicians, repair requests, Telegram, files, notifications,
reviews, dashboards, and business audit logs are intentionally not implemented.

## Stack

- Java 21
- Gradle
- Spring Boot 4.1.0, preserved from the generated repository
- PostgreSQL
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

Protected API requests use `Authorization: Bearer <accessToken>`. Refresh
tokens are opaque one-time tokens and are stored only as SHA-256 hashes.

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
