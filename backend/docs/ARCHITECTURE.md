# Architecture

## Current Phase

Phase 1 establishes authentication and user management on top of the backend
foundation. RepairAuto workflow modules such as customers, technicians, repair
requests, Telegram, files, notifications, reviews, dashboards, and business
audit logs are not implemented in this phase.

## Application Shape

The application is a Spring Boot web backend using PostgreSQL as the
authoritative database. Schema changes are owned by Flyway migrations. Hibernate
is configured with `ddl-auto=validate` so it does not create or alter the schema
implicitly.

## Package Conventions

- `common.api`: reusable API DTOs and pagination models
- `common.error`: standard API errors and exception translation
- `config`: framework and application configuration
- `observability`: request tracing and logging support
- `auth`: authentication endpoints, password policy, JWT access tokens, refresh
  sessions, and bootstrap admin creation
- `security`: stateless Spring Security adapters and JWT request authentication
- `user`: admin-managed user accounts and role/activation rules

Future phases should add business packages without bypassing these boundaries.
Controllers should call application services, not repositories directly.
Telegram handlers should call the same application services as REST adapters.

## Persistence Conventions

Mutable aggregate roots use explicit identifiers, UTC timestamps, and optimistic
locking where needed. Entities are not exposed directly as API DTOs.

Phase 1 creates `users` and `refresh_sessions`. Refresh tokens are stored only
as SHA-256 hashes. Refresh sessions keep token-family metadata so reuse can
revoke the whole family.

## Security Foundation

Spring Security is enabled. Health, info, OpenAPI documentation when enabled,
and public auth endpoints are anonymous. Protected application APIs require a
Bearer JWT access token.

The REST foundation is stateless: CSRF, form login, HTTP Basic, logout, and the
session request cache are disabled. Security context is request-local only and
is not persisted in HTTP sessions. No default Spring user is created.

Roles are `ADMIN` and `MANAGER`. User-management APIs are restricted to
`ADMIN`. The system prevents removing or disabling the last active admin and
prevents an admin from disabling their own account.

## Observability

Every HTTP request gets an `X-Trace-Id`. A valid incoming trace ID is preserved;
otherwise the backend generates one. The trace filter is ordered before Spring
Security so security-generated errors use the same trace ID in the response
header, logging MDC, and error body. MDC is cleared in a `finally` path.
