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
- Security-chain tests for trace IDs, stateless denial responses, absence of
  default users, Bearer JWT authentication, and role-based authorization
- Authentication integration tests for login, refresh rotation, refresh reuse
  detection, logout, logout-all, and password changes
- User-management integration tests for admin-only account CRUD, role changes,
  activation changes, last-active-admin protections, and session revocation
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

Future target:

- Overall line coverage at least 80%
- Core domain and application services at least 90%
- Critical business-rule branches explicitly tested

Coverage is secondary to meaningful behavior and invariant tests.
