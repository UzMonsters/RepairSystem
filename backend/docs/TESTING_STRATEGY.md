# Testing Strategy

## Verification Command

```bash
./gradlew clean check bootJar
```

This command compiles code, runs tests, applies Checkstyle, generates JaCoCo
coverage, and builds the executable jar. PostgreSQL integration tests require
Docker because they run PostgreSQL through Testcontainers.

Targeted Gradle test commands should be run sequentially in this workspace.
Running multiple `test` tasks concurrently can cause Gradle binary test-result
file collisions.

## Phase Test Categories

- Unit tests for isolated infrastructure behavior
- MVC/API tests for validation, error responses, HTTP status codes, and trace IDs
- PostgreSQL integration tests using Testcontainers
- Migration-from-zero tests against a fresh PostgreSQL database
- Configuration tests for restrictive defaults and invalid settings
- Docker Compose smoke checks where Docker is available
- MinIO/S3-compatible storage integration tests for Phase 6 attachments
- Security-chain tests for trace IDs, stateless denial responses, absence of
  default users, Bearer JWT authentication, and role-based authorization
- Authentication integration tests for login, refresh rotation, refresh reuse
  detection, logout, logout-all, and password changes
- Authentication-version integration tests for stale access-token rejection
  after password changes, role changes, deactivation, admin revocation, and
  logout-all
- User-management integration tests for admin-only account CRUD, role changes,
  activation changes, last-active-admin protections, and session revocation
- User-list pagination and sorting integration tests for whitelisted fields,
  bounds, and stable `INVALID_REQUEST_PARAMETER` responses
- Customer, technician, and category integration tests for CRUD-style
  management, activation/archive behavior, duplicate normalized values,
  authorization, no hard-delete endpoints, and list filtering/sorting
- Phase 2 schema tests for tables, unique constraints, check constraints,
  indexes, nullable Telegram uniqueness, and Phase 1 user compatibility
- Repair-request integration tests for creation, backend-controlled status and
  source, active customer/category rules, list search/filter/sort validation,
  historical archived-reference reads, intake update editability, customer
  history, no hard-delete endpoint, and concurrent request-number uniqueness
- Phase 3 schema tests for the repair-request table, sequence, foreign keys,
  check constraints, uniqueness, and indexes
- Phone normalization unit tests for supported Uzbekistan input forms and
  stable invalid-phone errors
- Bootstrap concurrency integration tests using PostgreSQL advisory locks
- Serialization tests proving sensitive user, customer, and technician entity
  fields are not emitted
- Timing-hardening tests proving missing-user login invokes the dummy BCrypt
  match path
- Focused unit tests for password policy and JWT validation failures

## Regression Rule

Every future phase must run:

1. Tests created for the current phase
2. All unit tests
3. All integration tests
4. All migration tests
5. Build/package verification
6. Static analysis configured in the repository
7. Relevant API smoke tests

The next phase must not start if compilation, tests, migrations, startup, or
critical security/business checks fail.

## Coverage

JaCoCo generates HTML and XML reports. No global threshold is enforced yet.

Targets:

- Overall line coverage at least 85%
- Core domain and application services at least 90%
- Critical business-rule branches explicitly tested

Coverage is secondary to meaningful behavior and invariant tests.
