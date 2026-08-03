# Architecture

## Current Phase

Phase 12 hardens the completed backend foundation for production release.
Authentication, user management, reference data, request intake, technician
assignment and scheduling, execution lifecycle, attachments, Telegram workflows,
transactional notifications, customer reviews, and dashboard analytics are
implemented. Production hardening adds strict `prod` startup validation,
DB-backed authentication throttling, cleanup jobs for stale technical data,
security headers, operational metrics, a non-root container runtime, release-gate
CI, and operator documentation. Pricing, payments, spare-parts inventory, and
business audit logs are intentionally not implemented.

## Application Shape

The application is a Spring Boot web backend using PostgreSQL as the
authoritative database. Schema changes are owned by Flyway migrations. Hibernate
is configured with `ddl-auto=validate` so it does not create or alter the schema
implicitly.

## Package Conventions

RepairAuto uses a pragmatic modular-monolith layout: business modules sit at
the top level, and larger modules contain internal technical layers.

- `identity.api`: REST adapters for authentication and user management
- `identity.api.dto`: identity request/response DTOs and mappers
- `identity.application`: authentication, refresh-session, password, bootstrap,
  and user-management use cases
- `identity.domain`: identity entities and domain enums
- `identity.infrastructure.persistence`: Spring Data repositories for identity
- `identity.infrastructure.security`: JWT and Spring Security identity adapters
- `customer.api/application/domain/infrastructure`: customer management
- `technician.api/application/domain/infrastructure`: technician management
- `catalog.category.api/application/domain/infrastructure`: repair-category
  management
- `repair.request.api/application/domain/infrastructure`: repair-request intake
  and customer history
- `repair.assignment.api/application/domain/infrastructure`: technician
  assignment, scheduling, history, and workload
- `repair.execution.api/application/domain/infrastructure`: repair lifecycle,
  execution details, and request status history
- `repair.attachment.api/application/domain/infrastructure`: repair attachment
  metadata, validation, S3-compatible storage access, download URLs, and soft
  deletion
- `shared.config`: framework and application configuration
- `shared.error`: standard API errors and exception translation
- `shared.observability`: request tracing and logging support
- `shared.pagination`: reusable pagination API models
- `shared.phone`: Uzbekistan MVP phone normalization shared by reference data
- `shared.i18n`: shared `LanguageCode` enum for `EN`, `RU`, and `UZ`
- `shared.cleanup`: scheduled cleanup for stale upload metadata, purged
  attachment objects, expired refresh sessions, Telegram updates, notification
  attempts, and auth throttle entries

Future phases should add modules such as `repair`, `telegram`, and
`notification` without bypassing these boundaries. Controllers and external
adapters should call application services, not repositories directly. Telegram
handlers should call the same application services as REST adapters.

## Persistence Conventions

Mutable aggregate roots use explicit identifiers, UTC timestamps, and optimistic
locking where needed. Entities are not exposed directly as API DTOs.

Phase 1 creates `users` and `refresh_sessions`. Phase 2 adds `customers`,
`technicians`, and `repair_categories`; forward migration V8 makes `EN`, `RU`,
and `UZ` the shared language set and expands categories to trilingual names and
descriptions. Phase 3 adds `repair_requests` and the
`repair_request_number_seq` sequence. Phase 4 adds `repair_assignments` with a
partial unique index protecting one active assignment per request. Phase 5 adds
`repair_executions` and append-only `repair_request_status_history`. Phase 6
adds `repair_attachments` for metadata only; object bytes are stored outside
PostgreSQL. Phase 7 and Phase 8 add customer and technician Telegram state.
Phase 9 adds notification outbox and delivery-attempt tables. Phase 10 adds
reviews. Phase 11 adds dashboard query indexes. Phase 12 adds
`auth_throttle_entries`, attachment object-purge tracking, and cleanup indexes.
Refresh tokens are stored only as SHA-256 hashes. Refresh sessions keep
token-family metadata so reuse can revoke the whole family.

Users also store `auth_version`, a positive bigint used for deterministic JWT
access-token invalidation. The JWT carries `authVersion`; authentication loads
the current user from PostgreSQL and rejects the token when the claim differs
from the database value. Security-sensitive changes use an atomic database
increment (`auth_version = auth_version + 1`) rather than timestamp boundary
checks.

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

Customer and technician APIs require `ADMIN` or `MANAGER`. Repair-category read
APIs require `ADMIN` or `MANAGER`; category create, update, activation, and
reorder APIs require `ADMIN`.

Repair-request, assignment, execution, attachment, notification, review, and
dashboard administrative APIs require `ADMIN` or `MANAGER`. Customer-facing and
technician-facing workflows are exposed through Telegram adapters, not public
REST accounts. Request status and source are backend-controlled; assignment
workflow may move requests only among
`NEW`, `ASSIGNED`, and `SCHEDULED`, while execution workflow controls
`IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`, and `CANCELLED`. No
repair-request delete endpoint exists.

Bootstrap admin initialization is guarded by PostgreSQL advisory transaction
lock `834645201180001`, which protects multi-instance startup. JVM-local locks
are not used for this cross-instance invariant.

The `prod` profile fails startup when required secrets, storage configuration,
CORS origins, JWT lifetime, timezone, or Flyway safety settings are invalid.
Authentication throttling uses PostgreSQL rows so limits are shared across
backend instances.

## Observability

Every HTTP request gets an `X-Trace-Id`. A valid incoming trace ID is preserved;
otherwise the backend generates one. The trace filter is ordered before Spring
Security so security-generated errors use the same trace ID in the response
header, logging MDC, and error body. MDC is cleared in a `finally` path.
