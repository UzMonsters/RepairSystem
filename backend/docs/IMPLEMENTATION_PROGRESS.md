# Implementation Progress

## Current Phase

Phase 1 - Authentication and User Management

## Completed Capabilities

- Phase 0 backend foundation retained.
- `users` table with email uniqueness, roles, activation state, timestamps, and
  optimistic locking.
- `refresh_sessions` table with hashed tokens, token families, rotation state,
  revocation metadata, and expiry metadata.
- Bootstrap admin creation when explicitly enabled and no admin exists.
- BCrypt password hashing and password policy enforcement.
- JWT access token issuing and validation.
- Opaque refresh token generation, SHA-256 token hashing, one-time rotation,
  logout, logout-all, and family revocation on reuse detection.
- Stateless Bearer-token request authentication.
- `/api/v1/auth/**` endpoints for login, refresh, logout, current user, password
  changes, and session revocation.
- `/api/v1/users/**` admin-only user-management endpoints.
- Last-active-admin and self-disable protections.
- OpenAPI bearer security scheme and protected operation markers.
- Docker Compose and `.env.example` auth/bootstrap configuration.

## Not Implemented

- Customers, technicians, and categories
- Repair requests
- Assignments and scheduling
- Repair lifecycle
- Telegram workflows
- Files and attachments
- Notifications
- Reviews
- Dashboard and analytics
- Business audit logs

## Migrations Added

- `V1__phase0_foundation.sql`
- `V2__create_users.sql`
- `V3__create_refresh_sessions.sql`
- `V4__add_phase1_indexes_and_constraints.sql`

## Tests Added Or Expanded

- Authentication integration tests for login, refresh rotation, refresh reuse
  detection, logout idempotency, disabled-user login denial, and password
  changes.
- User-management integration tests for create, update, list/search/filter,
  role changes, activation changes, session revocation, last-active-admin
  protection, and self-disable protection.
- Security integration tests using real Bearer JWTs and manager/admin role
  enforcement.
- OpenAPI integration tests for bearer security metadata.
- Unit tests for password policy and JWT validation failures.

## Tests Executed

- `./gradlew.bat test --tests com.example.darks.repair_auto.auth.AuthIntegrationTest --console plain`: passed
- `./gradlew.bat test --tests com.example.darks.repair_auto.user.UserManagementIntegrationTest --console plain`: passed
- `./gradlew.bat test --tests com.example.darks.repair_auto.integration.SecurityFoundationIntegrationTest --tests com.example.darks.repair_auto.integration.OpenApiIntegrationTest --console plain`: passed
- `./gradlew.bat test --tests com.example.darks.repair_auto.auth.service.PasswordPolicyTest --tests com.example.darks.repair_auto.security.JwtTokenServiceTest --console plain`: passed
- `./gradlew.bat test --tests com.example.darks.repair_auto.auth.service.BootstrapAdminRunnerTest --console plain`: passed
- `./gradlew.bat checkstyleMain checkstyleTest --console plain`: passed
- `./gradlew.bat clean check bootJar --console plain`: passed
- `docker compose config --quiet`: passed
- Docker Compose smoke on `SERVER_PORT=18081` and `POSTGRES_PORT=15433`:
  passed. `/actuator/health` returned `UP`, bootstrap admin login worked,
  `/auth/me` returned `ADMIN`, refresh rotated the token, old refresh returned
  `401`, logout returned success, anonymous `/api/v1/users` returned `401`,
  manager `/api/v1/users` returned `403`, and the smoke stack was removed with
  volumes.

## Test Results

The full Gradle verification command passed with 64 tests, 0 failures, 0 errors,
and 0 skipped tests. PostgreSQL integration tests used Testcontainers, Flyway
migrated a fresh PostgreSQL database, Checkstyle passed, JaCoCo generated
reports, and `bootJar` produced the executable jar.

## Coverage

JaCoCo report generated at `build/reports/jacoco/test/html/index.html`.

- Instruction coverage: 87.31%
- Branch coverage: 66.48%
- Line coverage: 87.73%

## Known Limitations

- Bootstrap admin is disabled by default; operators must enable it explicitly
  for initial setup.
- Business audit logs are not implemented until a later phase.
- Access-token invalidation after password changes relies on
  `passwordChangedAt`; refresh sessions are revoked immediately.
- The workspace is not currently a Git repository, so git metadata cannot be
  verified here.

## Next Permitted Phase

Phase 2 - Customers, technicians, and repair-request domain foundations.
