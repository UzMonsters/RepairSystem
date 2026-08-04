# Implementation Progress

## Current Phase

Phase 6 - Files and Repair Attachments

## Completed Capabilities

- Phase 0 backend foundation retained.
- `users` table with email uniqueness, roles, activation state, timestamps, and
  optimistic locking.
- `refresh_sessions` table with hashed tokens, token families, rotation state,
  revocation metadata, and expiry metadata.
- Bootstrap admin creation when explicitly enabled and no admin exists.
- PostgreSQL advisory-lock protection for concurrent bootstrap startup.
- BCrypt password hashing and password policy enforcement.
- Missing-user login timing-hardening with a precomputed dummy BCrypt hash.
- JWT access token issuing and validation.
- Deterministic access-token invalidation with user `authVersion`.
- Opaque refresh token generation, SHA-256 token hashing, one-time rotation,
  logout, logout-all, and family revocation on reuse detection.
- Stateless Bearer-token request authentication.
- `/api/v1/auth/**` endpoints for login, refresh, logout, current user, password
  changes, and session revocation.
- `/api/v1/users/**` admin-only user-management endpoints.
- Last-active-admin and self-disable protections.
- Explicit user-list pagination/sort validation with a public field whitelist.
- Password-bearing OpenAPI schemas marked `writeOnly`.
- `User` entity serialization guards for password hash and auth version.
- OpenAPI bearer security scheme and protected operation markers.
- Docker Compose and `.env.example` auth/bootstrap configuration.
- Package architecture refactored to the selected Option 2 modular monolith:
  `identity` for authentication/user-management concerns and `shared` for
  cross-cutting backend infrastructure.
- `customers` table and `/api/v1/customers` management APIs.
- `technicians` table and `/api/v1/technicians` management APIs.
- `repair_categories` table and `/api/v1/categories` read/admin-management
  APIs.
- Shared Uzbekistan MVP phone normalization to `+998XXXXXXXXX`.
- Shared `LanguageCode` enum for exactly `EN`, `RU`, and `UZ`.
- Customer and technician preferred-language support for `EN`, `RU`, and `UZ`
  with `UZ` as the staff-created default.
- Trilingual repair-category names and descriptions: `nameEn`, `nameRu`,
  `nameUz`, `descriptionEn`, `descriptionRu`, `descriptionUz`.
- Category name normalization for case-insensitive uniqueness.
- Activation/archive behavior without hard-delete endpoints.
- Phase 2 security matrix: customers and technicians for `ADMIN`/`MANAGER`,
  category reads for `ADMIN`/`MANAGER`, category writes for `ADMIN`.
- `repair_requests` table and PostgreSQL-backed `repair_request_number_seq`.
- `/api/v1/requests` APIs for staff request creation, listing, details, and
  editable intake update.
- `/api/v1/customers/{customerId}/requests` customer repair-history API.
- Backend-controlled request number, `status=NEW`, `source=ADMIN`, and creator.
- Active customer/category validation for new selections while preserving
  historical visibility after archiving.
- Description, location, preferred-visit, date-range, pagination, and sort
  validation for request intake.
- `repair_assignments` table for assignment history with one active assignment
  per request enforced by a PostgreSQL partial unique index.
- `/api/v1/requests/{requestId}/assign`, `/reassign`, `/unassign`,
  `/schedule`, `/assignment/accept`, `/assignment/reject`, and `/assignments`
  APIs for staff assignment workflow.
- `/api/v1/technicians/{technicianId}/workload` API with pending, accepted,
  total active, remaining capacity, and availability fields.
- Current assignment summary on request detail responses.
- Technician capacity validation using active `PENDING` and `ACCEPTED`
  assignments.
- Transactional assignment/reassignment/unassignment/rejection state changes
  that keep request status synchronized with assignment state.
- `repair_executions` table and `/api/v1/requests/{requestId}/execution`
  details API.
- Append-only `repair_request_status_history` table and
  `/api/v1/requests/{requestId}/status-history` API.
- Lifecycle APIs for start, diagnosis, wait for parts, resume, complete, and
  cancel.
- Completion and cancellation close active assignments as `COMPLETED` or
  `CANCELLED`, releasing technician workload capacity.
- Phase 3 request creation and Phase 4 assignment/scheduling transitions now
  append request status history rows.
- `repair_attachments` table for attachment metadata while binaries are stored
  in private S3-compatible object storage.
- MinIO-backed local Docker storage and S3-compatible `ObjectStorageService`
  abstraction.
- `/api/v1/requests/{requestId}/attachments` upload/list APIs and
  `/api/v1/attachments/{attachmentId}` detail/download/delete APIs.
- Multipart upload reservation, streaming object upload, metadata finalization,
  soft deletion, and failure compensation.
- File signature validation for JPEG, PNG, WebP, and PDF.
- Completion now requires at least one available `COMPLETION_PHOTO`.

## Not Implemented

- Telegram workflows
- Notifications
- Reviews
- Dashboard and analytics
- Pricing and payments
- Spare-parts inventory
- Business audit logs

## Migrations Added

- `V1__phase0_foundation.sql`
- `V2__create_users.sql`
- `V3__create_refresh_sessions.sql`
- `V4__add_phase1_indexes_and_constraints.sql`
- `V5__add_user_auth_version.sql`
- `V6__create_phase2_reference_data.sql`
- `V7__create_repair_requests.sql`
- `V8__add_core_language_support.sql`
- `V9__create_repair_assignments.sql`
- `V10__create_repair_execution_lifecycle.sql`
- `V11__create_repair_attachments.sql`

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
- Authentication-version integration tests for immediate stale-token rejection.
- User pagination/sort integration tests for valid fields and safe 400 errors.
- Bootstrap concurrency integration tests using real PostgreSQL transactions.
- Last-active-admin HTTP/service concurrency tests.
- Login timing-hardening and user serialization safety tests.
- Phone normalization unit tests.
- Customer integration tests for creation, duplicate normalized phones,
  filtering, sorting, pagination bounds, update, archive/reactivation,
  anonymous denial, no hard-delete endpoint, and concurrent duplicate creation.
- Technician integration tests for creation, default maximum concurrency,
  duplicate normalized phones, filtering, sorting, pagination bounds, update,
  deactivation/reactivation, anonymous denial, no hard-delete endpoint, and
  concurrent duplicate creation.
- Repair-category integration tests for admin writes, manager read-only access,
  duplicate normalized names, filtering, default display-order sorting,
  invalid/sensitive sorting, archive/reactivation, transactional reorder,
  no hard-delete endpoint, and concurrent duplicate creation.
- Phase 2 schema tests for tables, constraints, indexes, nullable Telegram
  uniqueness, and Phase 1 user compatibility.
- Customer and technician serialization safety tests.
- Repair-request integration tests for create/list/detail/update/history,
  security, archived-reference behavior, and concurrent request-number
  generation.
- Phase 3 schema tests for table, sequence, constraints, foreign keys, and
  indexes.
- Repair-assignment integration tests for assignment, scheduling,
  acceptance/rejection, unassignment, reassignment, history, workload,
  archived-technician visibility, security, and PostgreSQL concurrency cases.
- Phase 4 schema tests for assignment table, constraints, indexes, and partial
  unique active-assignment protection.
- Repair-execution integration tests for start, diagnosis, wait/resume,
  completion, cancellation, status history, workload release, security, and
  PostgreSQL concurrency cases.
- Phase 5 schema tests for execution table, status-history table, constraints,
  indexes, and assignment status extension.
- Attachment integration tests for upload/list/download/delete, validation,
  lifecycle rules, completion-photo requirement, concurrency, and security.
- S3-compatible MinIO integration test for upload, presigned download URL, and
  object deletion.
- Phase 6 schema tests for attachment table, constraints, foreign keys, and
  indexes.

## Tests Executed

- `./gradlew.bat compileJava compileTestJava --console plain`: passed after
  Phase 6 implementation.
- `./gradlew.bat test --tests "com.example.darks.repair_auto.repair.attachment.AttachmentIntegrationTest" --tests "com.example.darks.repair_auto.repair.attachment.infrastructure.storage.S3ObjectStorageServiceIntegrationTest" --tests "com.example.darks.repair_auto.integration.Phase6SchemaIntegrationTest" --tests "com.example.darks.repair_auto.repair.execution.RepairExecutionIntegrationTest" --console plain`:
  passed during Phase 6 implementation.
- `./gradlew.bat clean check bootJar --console plain`: passed after Phase 6
  implementation.
- `docker compose config --quiet`: passed after Phase 6 implementation.
- `docker compose build --quiet`: passed after Phase 6 implementation.
- Docker Compose smoke on `SERVER_PORT=18086`, `POSTGRES_PORT=15438`, and
  `MINIO_PORT=19000`: passed. The smoke flow verified backend, PostgreSQL, and
  MinIO health; bootstrap admin login; customer, category, technician, and
  request creation; customer-problem photo upload; assignment and acceptance;
  repair start; diagnosis; completion rejection without completion photo;
  completion-photo upload; authorized download URL and object download;
  completion; terminal attachment delete rejection; workload release; preterminal
  delete; unsupported file `400`; oversized file `400`; anonymous upload `401`;
  trace header preservation; private bucket access; and cleanup with
  `docker compose -p repairauto_phase6_smoke down --volumes`.
- `./gradlew.bat compileJava compileTestJava --console plain`: passed after
  Phase 5 implementation.
- `./gradlew.bat test --tests "com.example.darks.repair_auto.repair.execution.RepairExecutionIntegrationTest" --tests "com.example.darks.repair_auto.integration.Phase5SchemaIntegrationTest" --console plain`:
  passed during Phase 5 implementation.
- `./gradlew.bat clean check bootJar --console plain`: passed after Phase 5
  implementation.
- `docker compose config --quiet`: passed after Phase 5 implementation.
- `docker compose build --quiet`: passed after Phase 5 implementation.
- Docker Compose smoke on `SERVER_PORT=18085` and `POSTGRES_PORT=15437`:
  passed. The smoke flow verified health, bootstrap admin login, customer,
  trilingual category, technician, and request creation, assignment to
  `ASSIGNED`, assignment acceptance, repair start to `IN_PROGRESS`,
  EN/RU/UZ diagnosis text, wait for parts, resume, completion to `COMPLETED`,
  request detail execution summary, newest-first status history, anonymous
  `401`, and cleanup with `docker compose -p repairauto_phase5_smoke down
  --volumes`.
- `./gradlew.bat test --tests com.example.darks.repair_auto.repair.assignment.RepairAssignmentIntegrationTest --tests com.example.darks.repair_auto.integration.Phase4SchemaIntegrationTest --console plain`: passed during Phase 4 implementation.
- `./gradlew.bat clean check bootJar --console plain`: passed after Phase 4
  implementation.
- `docker compose config --quiet`: passed after Phase 4 implementation.
- `docker compose build --quiet`: passed after Phase 4 implementation.
- Docker Compose smoke on `SERVER_PORT=18084` and `POSTGRES_PORT=15436`:
  passed. The smoke flow verified health, bootstrap admin login, customer,
  trilingual category, technician, and request creation, assignment to
  `ASSIGNED`, scheduling to `SCHEDULED`, assignment acceptance, workload,
  reassignment, assignment history, rejection back to `NEW`, inactive
  technician rejection, capacity-exceeded rejection, anonymous `401`, trace ID
  consistency, no `Set-Cookie`, and cleanup with
  `docker compose down --volumes`.
- `./gradlew.bat test --tests com.example.darks.repair_auto.identity.AuthIntegrationTest --console plain`: passed
- `./gradlew.bat test --tests com.example.darks.repair_auto.identity.UserManagementIntegrationTest --console plain`: passed
- `./gradlew.bat test --tests com.example.darks.repair_auto.integration.SecurityFoundationIntegrationTest --tests com.example.darks.repair_auto.integration.OpenApiIntegrationTest --console plain`: passed
- `./gradlew.bat test --tests com.example.darks.repair_auto.identity.application.PasswordPolicyTest --tests com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenServiceTest --console plain`: passed
- `./gradlew.bat test --tests com.example.darks.repair_auto.identity.application.BootstrapAdminRunnerTest --console plain`: passed
- `./gradlew.bat checkstyleMain checkstyleTest --console plain`: passed
- `./gradlew.bat check -x test bootJar --console plain`: passed after
  stabilization edits.
- `./gradlew.bat test --tests com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenServiceTest --tests com.example.darks.repair_auto.identity.application.AuthenticationServiceTimingHardeningTest --tests com.example.darks.repair_auto.identity.UserSerializationTest --tests com.example.darks.repair_auto.identity.application.BootstrapAdminRunnerTest --console plain`: passed.
- `./gradlew.bat test --tests com.example.darks.repair_auto.identity.UserPaginationIntegrationTest --console plain`: passed.
- `./gradlew.bat test --tests com.example.darks.repair_auto.identity.AuthVersionIntegrationTest --console plain`: passed.
- `./gradlew.bat test --tests com.example.darks.repair_auto.identity.application.BootstrapAdminConcurrencyIntegrationTest --tests com.example.darks.repair_auto.identity.UserManagementIntegrationTest --console plain`: passed.
- `./gradlew.bat test --tests com.example.darks.repair_auto.integration.OpenApiIntegrationTest --tests com.example.darks.repair_auto.identity.AuthIntegrationTest --tests com.example.darks.repair_auto.integration.SecurityFoundationIntegrationTest --console plain`: passed.
- `./gradlew.bat clean check bootJar --console plain`: passed.
- `./gradlew.bat test --tests com.example.darks.repair_auto.customer.CustomerIntegrationTest --tests com.example.darks.repair_auto.technician.TechnicianIntegrationTest --console plain`: passed after Phase 2 stabilization.
- `./gradlew.bat clean check bootJar --console plain`: passed before Phase 3 implementation.
- `./gradlew.bat test --tests com.example.darks.repair_auto.repair.request.RepairRequestIntegrationTest --tests com.example.darks.repair_auto.integration.Phase3SchemaIntegrationTest --console plain`: passed during Phase 3 implementation.
- `./gradlew.bat clean check bootJar --console plain`: passed after Phase 3
  implementation.
- `./gradlew.bat tasks --all --console plain`: passed after Phase 3
  implementation.
- `docker compose config --quiet`: passed after Phase 3 implementation.
- `docker compose build --quiet`: passed after Phase 3 implementation.
- Docker Compose smoke on `SERVER_PORT=18083` and `POSTGRES_PORT=15435`:
  passed. The smoke flow verified health, bootstrap admin login, manager
  creation/login, active customer and category creation, repair-request
  creation, generated request number, `NEW` status, `ADMIN` source, details,
  search by request number, filter by customer, update preserving immutable
  fields, historical archived-customer visibility, archived-customer create
  rejection, historical archived-category visibility, archived-category create
  rejection, manager create/view access, anonymous `401`, invalid sort `400`,
  excessive page size `400`, trace ID consistency, no `Set-Cookie`, final
  health `UP`, and cleanup with `docker compose down --volumes`.
- `docker compose ps` and `docker ps --filter name=repairauto_phase3_smoke`:
  confirmed no Phase 3 smoke containers remained running.
- `./gradlew.bat tasks --all --console plain`: passed.
- `docker compose config --quiet`: passed.
- `docker compose build --quiet`: passed. A no-cache rebuild was also used for
  the isolated smoke image after Docker reused an older cached layer.
- `./gradlew.bat testClasses --console plain`: passed after the Option 2
  package refactor.
- `./gradlew.bat clean check bootJar --console plain`: passed after the Option
  2 package refactor.
- `./gradlew.bat test --tests com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizerTest --tests com.example.darks.repair_auto.customer.CustomerIntegrationTest --tests com.example.darks.repair_auto.technician.TechnicianIntegrationTest --tests com.example.darks.repair_auto.catalog.category.RepairCategoryIntegrationTest --console plain`: passed.
- `./gradlew.bat clean check bootJar --console plain`: passed after Phase 2
  implementation.
- `./gradlew.bat tasks --all --console plain`: passed after Phase 2
  implementation.
- `docker compose config --quiet`: passed after Phase 2 implementation.
- Root `docker compose -f docker-compose.yml config --quiet`: passed after
  Phase 2 implementation.
- `docker compose build --quiet`: passed after Phase 2 implementation.
- Docker Compose smoke on `SERVER_PORT=18082` and `POSTGRES_PORT=15434`:
  passed. The smoke flow verified health, admin login, manager creation/login,
  admin and manager customer creation, duplicate customer phone `409`, admin and
  manager technician creation, duplicate technician phone `409`, admin category
  creation, manager category listing, manager category creation `403`,
  archive/reactivation for representative records, invalid sort `400`,
  excessive page size `400`, no `Set-Cookie`, production OpenAPI default `404`,
  and trace header preservation on invalid sort.
- `docker compose down --volumes`: passed for the smoke stack.
- `docker compose ps` and `docker ps --filter name=repairauto_phase2_smoke`:
  confirmed no Phase 2 smoke containers remained running.
- `docker compose config --quiet`: passed
- Docker Compose smoke on `SERVER_PORT=18081` and `POSTGRES_PORT=15433`:
  passed. `/actuator/health` returned `UP`, bootstrap admin login worked,
  `/auth/me` returned `ADMIN`, refresh rotated the token, old refresh returned
  `401`, logout returned success, anonymous `/api/v1/users` returned `401`,
  manager `/api/v1/users` returned `403`, and the smoke stack was removed with
  volumes.

## Test Results

The full Gradle verification command passed with the expanded Phase 6 suite.
PostgreSQL integration tests used Testcontainers, Flyway migrated a fresh
PostgreSQL database through V11, Checkstyle passed, JaCoCo generated reports,
and `bootJar` produced the executable jar.

JUnit XML totals after Phase 6: 150 tests, 0 failures, 0 errors, 0 skipped.

## Coverage

JaCoCo report generated at `build/reports/jacoco/test/html/index.html`.

- Instruction coverage: 90.43%
- Branch coverage: 70.46%
- Line coverage: 90.17%

## Known Limitations

- Bootstrap admin is disabled by default; operators must enable it explicitly
  for initial setup.
- Business audit logs are not implemented until a later phase.
- `passwordChangedAt` remains metadata only. Access-token invalidation is based
  on `authVersion`.
- Telegram identity fields exist only as nullable future-integration storage;
  no Telegram bot, linking, or registration workflow exists in Phase 2.
- Dashboards, notifications, reviews, pricing, payments, spare-parts inventory,
  business audit logs, and Telegram workflow adapters remain deferred.
- Docker BuildKit may reuse cached layers aggressively on this Windows setup.
  Use `docker compose build --no-cache` if a smoke stack appears to run an older
  jar after source changes.

## Next Permitted Phase

Independent Phase 6 focused verification should run before Phase 7.
