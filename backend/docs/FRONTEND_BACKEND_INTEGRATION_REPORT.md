# RepairAuto Backend Completion and Frontend Integration Report

## 1. Document Information

| Field | Value |
| --- | --- |
| Project path | `C:\Users\User\Documents\RepairSystem\backend` |
| Purpose | Give the frontend developer the contract needed to connect the client to the backend. |
| API base path | `/api/v1` |
| Report date | 2026-08-03 |
| Backend source of truth | Controllers, DTOs, enums, migrations, config, and tests in this backend project. |
| Latest migration in source | `V17__production_hardening.sql` |
| Backend phase marker | `info.app.phase=12` |

Status vocabulary:

| Status | Meaning |
| --- | --- |
| IMPLEMENTED | Feature is present in the backend. |
| VERIFIED | Feature is covered by automated or documented manual verification. |
| FRONTEND INTEGRATION REQUIRED | Backend is complete; frontend must connect it. |
| BACKEND-ONLY | Operational capability not called directly by the web client. |
| OPTIONAL FOR MVP UI | Backend exists, but the first web UI can defer it. |

## 2. Executive Summary

The backend is ready for frontend integration. The client should connect to authentication first, then reference data, then the repair request workspace, then assignment, scheduling, execution, attachments, dashboards, reviews, and operational notification screens.

The frontend must treat the backend as authoritative for permissions, status transitions, technician capacity, file rules, completion evidence, and business conflicts. UI buttons can be hidden based on role/status, but every command must still handle backend `401`, `403`, `404`, `409`, `422`-style validation if added later, `429`, and `500` responses.

The frontend does not call the Telegram webhook. Telegram-created customers, requests, photos, reviews, and notification state must be displayed through normal authenticated REST APIs.

## 3. Completed Project Scope

| Backend capability | Status | Frontend action |
| --- | --- | --- |
| Staff authentication and refresh-token sessions | VERIFIED | FRONTEND INTEGRATION REQUIRED |
| Admin user management | VERIFIED | FRONTEND INTEGRATION REQUIRED |
| Customers | VERIFIED | FRONTEND INTEGRATION REQUIRED |
| Technicians and workload | VERIFIED | FRONTEND INTEGRATION REQUIRED |
| Service categories | VERIFIED | FRONTEND INTEGRATION REQUIRED |
| Repair request intake, list, detail, update, and history | VERIFIED | FRONTEND INTEGRATION REQUIRED |
| Assignment and scheduling lifecycle | VERIFIED | FRONTEND INTEGRATION REQUIRED |
| Repair execution lifecycle | VERIFIED | FRONTEND INTEGRATION REQUIRED |
| Attachments and completion evidence | VERIFIED | FRONTEND INTEGRATION REQUIRED |
| Customer and technician Telegram workflows | VERIFIED | Display resulting data; do not call webhook. |
| Notification outbox and retry | VERIFIED | OPTIONAL FOR MVP UI |
| Customer reviews | VERIFIED | OPTIONAL FOR MVP UI |
| Dashboard APIs | VERIFIED | OPTIONAL FOR MVP UI |
| Health, config validation, cleanup jobs, migrations, security headers | VERIFIED | BACKEND-ONLY except health indicators. |

## 4. Business Capabilities Delivered

Implemented business outcomes:

- Staff can log in securely, refresh sessions, log out, revoke all sessions, and change passwords.
- Administrators can create, edit, activate, deactivate, re-role, and revoke sessions for staff users.
- Customers can be created, searched, updated, archived/reactivated, linked to Telegram, and preserved historically.
- Technicians can be created, searched, updated, archived/reactivated, linked to Telegram, and checked for workload capacity.
- Service categories support EN/RU/UZ names, descriptions, display order, activation, and reorder.
- Staff can create repair requests for active customers and categories.
- Repair requests have generated request numbers, source, status, priority, customer/category summaries, current assignment, execution summary, search, filters, sorting, pagination, and customer history.
- Managers can assign, reassign, unassign, schedule, reschedule, clear schedule, accept, reject, and inspect assignment history.
- Staff can start repair work, record diagnosis, wait for parts, resume, complete, cancel, and inspect status history.
- Completion requires available `COMPLETION_PHOTO` evidence.
- Attachments support secure multipart upload, metadata, type rules, S3-compatible private storage, short-lived download URLs, and soft deletion.
- Telegram workflows can create customer-originated operational data and notification/review records.
- Operational APIs expose dashboard metrics, review summaries, and notification retry controls.

## 5. Technical Capabilities Delivered

| Business capability | Technical implementation |
| --- | --- |
| Staff authentication | Spring Security, `AuthController`, JWT access tokens, BCrypt passwords, auth throttling. |
| Refresh lifecycle | Opaque refresh tokens, SHA-256 token hashes, one-time rotation, family revocation on reuse. |
| User management | `UserManagementController`, role and activation commands, last-admin protection. |
| Customers | `CustomerController`, `CustomerService`, normalized Uzbekistan phone numbers. |
| Categories | `RepairCategoryController`, trilingual DTOs, normalized uniqueness, reorder command. |
| Technicians | `TechnicianController`, capacity fields, Telegram linking, workload endpoint. |
| Repair requests | `RepairRequestController`, PostgreSQL request number sequence, status/source/priority enums. |
| Assignment and scheduling | `RepairAssignmentController`, assignment history, active-assignment constraint, capacity validation. |
| Repair execution | `RepairExecutionController`, execution aggregate, append-only status history. |
| Attachments | `AttachmentController`, object storage abstraction, file signature validation, presigned downloads. |
| Telegram | Secured webhook, update idempotency, customer/staff session state machines. |
| Notifications | Durable PostgreSQL outbox, worker, retry policy, admin inspection endpoints. |
| Reviews | Review query and summary APIs. |
| Dashboards | Aggregated manager metrics using configured business timezone. |
| API conventions | Shared `PageResponse`, `ApiErrorResponse`, `ApiErrorCode`, `X-Trace-Id`. |

## 6. Business Actors and Permissions

| Actor | Backend role/source | Frontend behavior |
| --- | --- | --- |
| Administrator | `ADMIN` | Show all admin/staff features, including user and category management. |
| Manager | `MANAGER` | Show operations features: customers, technicians, requests, assignments, execution, attachments, dashboards, reviews, notifications. Hide admin-only user/category writes where denied. |
| Customer | Telegram identity / customer record | No web login in this backend. Display customer data inside staff UI. |
| Technician | Technician record, optional Telegram link | No separate web role in `UserRole`; technicians operate through staff UI or Telegram workflow. |
| Telegram webhook | Secret-protected public endpoint | BACKEND-ONLY. The web client must not call it. |
| Notification worker | Backend scheduled worker | BACKEND-ONLY. UI may inspect notification rows and retry eligible failures. |

## 7. Complete Repair Service Workflow

Frontend should implement the main workflow in this order:

1. Staff logs in and the client stores the latest access/refresh token pair.
2. Staff selects or creates an active customer.
3. Staff selects an active service category.
4. Staff creates a repair request. Backend returns generated `requestNumber`, `status=NEW`, `source=ADMIN`.
5. Manager assigns a technician, optionally with `scheduledVisitAt`.
6. Technician or staff accepts the assignment.
7. Staff starts repair. Backend moves status to `IN_PROGRESS`.
8. Staff records diagnosis.
9. If needed, staff moves request to `WAITING_FOR_PARTS`, then resumes it.
10. Staff uploads at least one `COMPLETION_PHOTO` while request is `IN_PROGRESS`.
11. Staff completes the repair. Backend closes active assignment and releases workload.
12. Customer may submit a Telegram review; staff UI can show reviews/dashboard data.

## 8. System Architecture

```text
Web client
  -> REST/JSON and multipart HTTP
  -> Authorization: Bearer <accessToken>
Spring Boot backend
  -> Trace ID filter
  -> JWT authentication filter
  -> Controllers and DTOs
  -> Service-layer business rules
  -> JPA repositories
PostgreSQL
  -> Flyway migrations V1-V17
Private object storage
  -> S3-compatible attachment bytes
Telegram API
  -> Webhook input and notification output
```

## 9. Backend Module Overview

| Module | Main API class | Status |
| --- | --- | --- |
| Authentication | `identity.api.AuthController` | VERIFIED |
| Users | `identity.api.UserManagementController` | VERIFIED |
| Customers | `customer.api.CustomerController` | VERIFIED |
| Technicians | `technician.api.TechnicianController` | VERIFIED |
| Technician Telegram link | `telegram.technician.api.TechnicianTelegramLinkController` | VERIFIED |
| Categories | `catalog.category.api.RepairCategoryController` | VERIFIED |
| Repair requests | `repair.request.api.RepairRequestController` | VERIFIED |
| Assignments | `repair.assignment.api.RepairAssignmentController` | VERIFIED |
| Execution | `repair.execution.api.RepairExecutionController` | VERIFIED |
| Attachments | `repair.attachment.api.AttachmentController` | VERIFIED |
| Telegram webhook | `telegram.core.api.TelegramWebhookController` | BACKEND-ONLY |
| Notifications | `notification.api.NotificationAdminController` | OPTIONAL FOR MVP UI |
| Reviews | `review.api.ReviewController` | OPTIONAL FOR MVP UI |
| Dashboard | `dashboard.api.DashboardController` | OPTIONAL FOR MVP UI |

## 10. Local Development Setup

Backend URL:

```text
http://localhost:8080
```

Run backend dependencies and app:

```bash
docker compose up --build
```

Useful local endpoints:

| Endpoint | Purpose |
| --- | --- |
| `GET /actuator/health` | Basic health. |
| `GET /actuator/health/liveness` | Liveness probe. |
| `GET /actuator/health/readiness` | Readiness probe. |
| `GET /actuator/info` | App info. |
| `GET /v3/api-docs` | OpenAPI JSON when enabled. |
| `GET /swagger-ui.html` | Swagger UI when enabled. |

## 11. Frontend Environment Configuration

Recommended client env:

```text
VITE_API_BASE_URL=http://localhost:8080
VITE_API_PREFIX=/api/v1
```

Backend/operator env that affects frontend:

| Variable | Client impact |
| --- | --- |
| `APP_CORS_ALLOWED_ORIGINS` | Must include the frontend origin. |
| `APP_OPENAPI_ENABLED` / `APP_SWAGGER_UI_ENABLED` | Enables API docs in local/dev only. |
| `APP_JWT_ACCESS_TOKEN_TTL` | Access token lifetime. Default is short; client must refresh. |
| `APP_REFRESH_TOKEN_TTL` | Refresh session lifetime. |
| `APP_AUTH_THROTTLE_*` | Login/refresh failures may return `429`. |
| `APP_STORAGE_MAX_FILE_SIZE` / `APP_STORAGE_MAX_REQUEST_SIZE` | Upload validation limits. |
| `APP_STORAGE_DOWNLOAD_URL_TTL` | Download URL expiration display. |
| `APP_DASHBOARD_BUSINESS_TIME_ZONE` | Dashboard grouping timezone, default expected for Uzbekistan operations. |

## 12. API Conventions

| Convention | Contract |
| --- | --- |
| Base path | `/api/v1` |
| JSON | lower camel case |
| Auth | `Authorization: Bearer <accessToken>` |
| Trace header | `X-Trace-Id`, echoed in response headers and error bodies |
| Dates | ISO 8601 offset date-time strings |
| Pagination | `page`, `size`, repeated `sort` values |
| Max page size | 100 on list APIs |
| Enums | Uppercase strings |
| Cookies | Not used for authentication |

Page response:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Error response:

```json
{
  "timestamp": "2026-08-03T10:00:00Z",
  "status": 409,
  "code": "TECHNICIAN_CAPACITY_EXCEEDED",
  "message": "Technician capacity exceeded.",
  "path": "/api/v1/requests/12/assign",
  "traceId": "support-trace-123",
  "fieldErrors": []
}
```

## 13. Authentication and Session Management

| Action | Endpoint | Frontend notes |
| --- | --- | --- |
| Login | `POST /auth/login` | Public. Store `accessToken`, `refreshToken`, expiry seconds, and `user`. |
| Refresh | `POST /auth/refresh` | Public. One-time refresh token rotates. Use single-flight refresh queue. |
| Logout | `POST /auth/logout` | Public with refresh token body. Clear local session. |
| Logout all | `POST /auth/logout-all` | Authenticated. Clear local session. |
| Current user | `GET /auth/me` | Authenticated. Use for session restore. |
| Change password | `PATCH /auth/password` | Authenticated. Backend revokes sessions; force login after success. |

Login request:

```json
{
  "email": "admin@example.com",
  "password": "ChangeMe123!"
}
```

Login response:

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque>",
  "tokenType": "Bearer",
  "accessTokenExpiresIn": 900,
  "refreshTokenExpiresIn": 2592000,
  "user": {
    "id": 1,
    "fullName": "System Administrator",
    "email": "admin@example.com",
    "role": "ADMIN"
  }
}
```

Refresh rule: only one request may use the current refresh token. If multiple API calls fail with expired access at once, queue them behind one refresh call, then retry them after the token pair is replaced.

## 14. User and Role Management

Base: `/api/v1/users`. Admin-only.

| Method | Path | Use |
| --- | --- | --- |
| `GET` | `/users` | List users with `search`, `role`, `active`, `page`, `size`, `sort`. |
| `GET` | `/users/{id}` | Get user details. |
| `POST` | `/users` | Create staff user. |
| `PUT` | `/users/{id}` | Update full name/email. |
| `PATCH` | `/users/{id}/role` | Change role and revoke sessions. |
| `PATCH` | `/users/{id}/activation` | Activate/deactivate and revoke sessions on disable. |
| `POST` | `/users/{id}/revoke-sessions` | Revoke all user sessions. |

Roles: `ADMIN`, `MANAGER`.

## 15. Customer Management

Base: `/api/v1/customers`. Requires `ADMIN` or `MANAGER`.

| Method | Path | Use |
| --- | --- | --- |
| `GET` | `/customers` | List/search/filter customers. |
| `GET` | `/customers/{id}` | Customer detail. |
| `POST` | `/customers` | Create customer. |
| `PUT` | `/customers/{id}` | Update customer. |
| `PATCH` | `/customers/{id}/activation` | Archive/reactivate. |

List filters: `search`, `phone`, `language`, `active`, `registrationSource`, `createdFrom`, `createdTo`, `page`, `size`, `sort`.

Create/update fields:

```json
{
  "fullName": "Ali Karimov",
  "phone": "+998 90 123 45 67",
  "preferredLanguage": "UZ"
}
```

Phones normalize to `+998XXXXXXXXX`. Display `telegramLinked` when returned.

## 16. Service Category Management

Base: `/api/v1/categories`.

| Method | Path | Role | Use |
| --- | --- | --- | --- |
| `GET` | `/categories` | `ADMIN`, `MANAGER` | List categories. |
| `GET` | `/categories/{id}` | `ADMIN`, `MANAGER` | Category detail. |
| `POST` | `/categories` | `ADMIN` | Create category. |
| `PUT` | `/categories/{id}` | `ADMIN` | Update category. |
| `PATCH` | `/categories/{id}/activation` | `ADMIN` | Archive/reactivate. |
| `PATCH` | `/categories/reorder` | `ADMIN` | Update display order. |

Create/update fields:

```json
{
  "nameEn": "Washing machine",
  "nameRu": "Stiralnaya mashina",
  "nameUz": "Kir yuvish mashinasi",
  "descriptionEn": "Home appliance repair",
  "descriptionRu": "Repair description",
  "descriptionUz": "Tamirlash tavsifi",
  "displayOrder": 10,
  "active": true
}
```

For request creation, frontend should show only `active=true` categories. Historical request details may still display archived categories.

## 17. Repair Request Management

Base: `/api/v1/requests`. Requires `ADMIN` or `MANAGER`.

| Method | Path | Use |
| --- | --- | --- |
| `GET` | `/requests` | List/search/filter/sort/paginate requests. |
| `GET` | `/requests/{id}` | Request detail. |
| `POST` | `/requests` | Staff request intake. |
| `PUT` | `/requests/{id}` | Update intake fields while status is `NEW`. |
| `GET` | `/customers/{customerId}/requests` | Customer repair history. |

List filters: `search`, `requestNumber`, `customerId`, `categoryId`, `status`, `priority`, `source`, `createdFrom`, `createdTo`, `preferredVisitFrom`, `preferredVisitTo`, `page`, `size`, `sort`.

Create/update body:

```json
{
  "customerId": 1,
  "categoryId": 2,
  "description": "Machine leaks water during spin cycle",
  "address": "Tashkent, Chilonzor",
  "latitude": null,
  "longitude": null,
  "priority": "NORMAL",
  "customerPreferredVisitAt": "2026-08-04T10:00:00+05:00",
  "internalNote": "Customer prefers morning visit"
}
```

Rules:

- Backend generates `requestNumber`.
- REST-created requests have `source=ADMIN`.
- Telegram-created requests have `source=TELEGRAM`.
- Only `NEW` requests are editable.
- New selections must use active customers and active categories.
- Location must include either address or both latitude and longitude.

## 18. Assignment and Scheduling

Requires `ADMIN` or `MANAGER`.

| Method | Path | Body | Refresh after success |
| --- | --- | --- | --- |
| `POST` | `/requests/{requestId}/assign` | `technicianId`, optional `scheduledVisitAt` | Request detail, assignments, workload. |
| `POST` | `/requests/{requestId}/reassign` | `technicianId`, optional `scheduledVisitAt`, `reason` | Request detail, assignments, old/new workload. |
| `POST` | `/requests/{requestId}/unassign` | `reason` | Request detail, assignments, workload. |
| `PATCH` | `/requests/{requestId}/schedule` | `scheduledVisitAt` or `clearSchedule=true` | Request detail, assignments. |
| `POST` | `/requests/{requestId}/assignment/accept` | none | Request detail, workload. |
| `POST` | `/requests/{requestId}/assignment/reject` | `reason` | Request detail, assignments, workload. |
| `GET` | `/requests/{requestId}/assignments` | none | Assignment history. |
| `GET` | `/technicians/{technicianId}/workload` | none | Workload widget. |

Technician capacity may change after the page loads. Handle `TECHNICIAN_CAPACITY_EXCEEDED` by refreshing request details and workload.

## 19. Repair Execution Lifecycle

Requires `ADMIN` or `MANAGER`.

| Method | Path | Body | Main status effect |
| --- | --- | --- | --- |
| `POST` | `/requests/{requestId}/start` | none | `ASSIGNED`/`SCHEDULED` to `IN_PROGRESS`. |
| `PATCH` | `/requests/{requestId}/diagnosis` | `diagnosis` | Updates execution details. |
| `POST` | `/requests/{requestId}/wait-for-parts` | `reason` | `IN_PROGRESS` to `WAITING_FOR_PARTS`. |
| `POST` | `/requests/{requestId}/resume` | optional `note` | `WAITING_FOR_PARTS` to `IN_PROGRESS`. |
| `POST` | `/requests/{requestId}/complete` | `workPerformed`, optional `completionNote` | `IN_PROGRESS` to `COMPLETED`. |
| `POST` | `/requests/{requestId}/cancel` | `reason` | Non-terminal to `CANCELLED`. |
| `GET` | `/requests/{requestId}/execution` | none | Execution detail. |
| `GET` | `/requests/{requestId}/status-history` | none | Newest-first status history. |

Completion requirements:

- Diagnosis must exist.
- `workPerformed` is required.
- At least one available `COMPLETION_PHOTO` must exist.

After every command, refresh request detail, execution detail, status history, current assignment, and technician workload if affected.

## 20. Attachments and Evidence

Requires `ADMIN` or `MANAGER`.

| Method | Path | Use |
| --- | --- | --- |
| `POST` | `/requests/{requestId}/attachments?type=COMPLETION_PHOTO` | Multipart upload with `file` part. |
| `GET` | `/requests/{requestId}/attachments?type=...` | List available attachments. |
| `GET` | `/attachments/{attachmentId}` | Attachment metadata. |
| `GET` | `/attachments/{attachmentId}/download-url` | Short-lived authorized download URL. |
| `DELETE` | `/attachments/{attachmentId}` | Soft-delete before terminal status, optional `reason`. |

Attachment types:

- `CUSTOMER_PROBLEM_PHOTO`
- `DIAGNOSIS_PHOTO`
- `COMPLETION_PHOTO`
- `GENERAL_DOCUMENT`

Supported content:

- Photos: JPEG, PNG, WebP.
- Documents: PDF, JPEG, PNG, WebP.

Frontend rules:

- Use `FormData`: `file` part plus `type` query parameter.
- Show upload progress.
- Treat upload as successful only when the backend returns an `AttachmentResponse`.
- Use backend download URL directly; it expires at `expiresAt`.
- Do not expose storage keys or object URLs from anywhere else.

## 21. Customer Telegram Bot

BACKEND-ONLY for webhook calls. The web client does not call `/api/v1/telegram/webhook`.

Frontend should display Telegram-originated data when it appears through regular APIs:

- Customer `registrationSource=TELEGRAM`.
- Customer `telegramLinked=true`.
- Request `source=TELEGRAM`.
- Customer preferred language.
- Customer photos as attachments.
- Reviews with `source=TELEGRAM`.
- Notification delivery state where operational UI includes notifications.

## 22. Staff Telegram Bot

Technician/staff Telegram linking is exposed for the staff UI:

| Method | Path | Use |
| --- | --- | --- |
| `POST` | `/technicians/{technicianId}/telegram-link` | Create link token/deep link. |
| `DELETE` | `/technicians/{technicianId}/telegram-link` | Unlink technician Telegram account. |

Response:

```json
{
  "deepLink": "https://t.me/...",
  "expiresAt": "2026-08-03T12:00:00Z"
}
```

## 23. Notifications and Background Workers

The notification worker is BACKEND-ONLY. Admin/operations UI may inspect and retry notifications.

| Method | Path | Use |
| --- | --- | --- |
| `GET` | `/notifications` | List outbox rows by status/type/recipient/request/date. |
| `GET` | `/notifications/{notificationId}` | Notification detail and attempts. |
| `POST` | `/notifications/{notificationId}/retry` | Retry eligible failed notification. |

This is OPTIONAL FOR MVP UI unless the client includes an admin operations panel.

## 24. Localization

Backend returns stable enum and error codes. Frontend should translate labels to English, Russian, and Uzbek.

Translate at minimum:

- `UserRole`
- `LanguageCode`
- `CustomerRegistrationSource`
- `RepairRequestStatus`
- `RepairRequestPriority`
- `RepairRequestSource`
- `AssignmentStatus`
- `AttachmentType`
- `AttachmentStatus`
- `NotificationStatus`
- `NotificationType`
- `ReviewSource`
- all known `ApiErrorCode` values used in client flows

## 25. Error Handling

Client behavior:

| HTTP status | Frontend handling |
| --- | --- |
| `400` | Bad request, validation, invalid filters, invalid sort/date range. Show field/form error. |
| `401` | If access expired, run one refresh and retry. If refresh fails, clear session. |
| `403` | Hide action or show permission-denied state. |
| `404` | Show not-found state and refresh parent list. |
| `409` | Business conflict. Refresh affected entity and show specific message. |
| `429` | Show throttling countdown/state; do not retry aggressively. |
| `500` | Generic error with trace ID. |

Always capture `traceId`.

## 26. Date, Time and Timezone Handling

- Backend serializes date-time fields as ISO 8601 offset timestamps.
- Backend stores timestamps in UTC.
- Dashboard grouping uses `APP_DASHBOARD_BUSINESS_TIME_ZONE`, expected for Uzbekistan operations.
- Frontend should send scheduled/preferred dates with explicit offsets, for example `2026-08-04T10:00:00+05:00`.
- Show request/audit timestamps in the selected UI timezone, but do not strip offsets from stored API values.

## 27. Security and Production Hardening

Frontend-relevant security rules:

- Do not use cookies for auth; backend expects bearer tokens.
- Do not call protected APIs without an access token.
- Do not decode JWT as the source of truth for permissions; use `/auth/me` and backend responses.
- Do not attempt parallel refresh calls.
- Do not expose refresh tokens in URLs, logs, analytics, or error reporting.
- Do not call Telegram webhook from the browser.
- Do not expose object storage bucket names, keys, or credentials.
- Configure production CORS to the exact frontend origin.

## 28. Complete API Catalogue

All paths below are relative to `/api/v1` unless noted.

| Area | Method | Path | Status |
| --- | --- | --- | --- |
| Auth | `POST` | `/auth/login` | FRONTEND INTEGRATION REQUIRED |
| Auth | `POST` | `/auth/refresh` | FRONTEND INTEGRATION REQUIRED |
| Auth | `POST` | `/auth/logout` | FRONTEND INTEGRATION REQUIRED |
| Auth | `POST` | `/auth/logout-all` | FRONTEND INTEGRATION REQUIRED |
| Auth | `GET` | `/auth/me` | FRONTEND INTEGRATION REQUIRED |
| Auth | `PATCH` | `/auth/password` | FRONTEND INTEGRATION REQUIRED |
| Users | `GET` | `/users` | FRONTEND INTEGRATION REQUIRED |
| Users | `GET` | `/users/{id}` | FRONTEND INTEGRATION REQUIRED |
| Users | `POST` | `/users` | FRONTEND INTEGRATION REQUIRED |
| Users | `PUT` | `/users/{id}` | FRONTEND INTEGRATION REQUIRED |
| Users | `PATCH` | `/users/{id}/role` | FRONTEND INTEGRATION REQUIRED |
| Users | `PATCH` | `/users/{id}/activation` | FRONTEND INTEGRATION REQUIRED |
| Users | `POST` | `/users/{id}/revoke-sessions` | FRONTEND INTEGRATION REQUIRED |
| Customers | `GET` | `/customers` | FRONTEND INTEGRATION REQUIRED |
| Customers | `GET` | `/customers/{id}` | FRONTEND INTEGRATION REQUIRED |
| Customers | `POST` | `/customers` | FRONTEND INTEGRATION REQUIRED |
| Customers | `PUT` | `/customers/{id}` | FRONTEND INTEGRATION REQUIRED |
| Customers | `PATCH` | `/customers/{id}/activation` | FRONTEND INTEGRATION REQUIRED |
| Categories | `GET` | `/categories` | FRONTEND INTEGRATION REQUIRED |
| Categories | `GET` | `/categories/{id}` | FRONTEND INTEGRATION REQUIRED |
| Categories | `POST` | `/categories` | FRONTEND INTEGRATION REQUIRED |
| Categories | `PUT` | `/categories/{id}` | FRONTEND INTEGRATION REQUIRED |
| Categories | `PATCH` | `/categories/{id}/activation` | FRONTEND INTEGRATION REQUIRED |
| Categories | `PATCH` | `/categories/reorder` | FRONTEND INTEGRATION REQUIRED |
| Technicians | `GET` | `/technicians` | FRONTEND INTEGRATION REQUIRED |
| Technicians | `GET` | `/technicians/{id}` | FRONTEND INTEGRATION REQUIRED |
| Technicians | `POST` | `/technicians` | FRONTEND INTEGRATION REQUIRED |
| Technicians | `PUT` | `/technicians/{id}` | FRONTEND INTEGRATION REQUIRED |
| Technicians | `PATCH` | `/technicians/{id}/activation` | FRONTEND INTEGRATION REQUIRED |
| Technician Telegram | `POST` | `/technicians/{technicianId}/telegram-link` | OPTIONAL FOR MVP UI |
| Technician Telegram | `DELETE` | `/technicians/{technicianId}/telegram-link` | OPTIONAL FOR MVP UI |
| Requests | `GET` | `/requests` | FRONTEND INTEGRATION REQUIRED |
| Requests | `GET` | `/requests/{id}` | FRONTEND INTEGRATION REQUIRED |
| Requests | `POST` | `/requests` | FRONTEND INTEGRATION REQUIRED |
| Requests | `PUT` | `/requests/{id}` | FRONTEND INTEGRATION REQUIRED |
| Requests | `GET` | `/customers/{customerId}/requests` | FRONTEND INTEGRATION REQUIRED |
| Assignments | `POST` | `/requests/{requestId}/assign` | FRONTEND INTEGRATION REQUIRED |
| Assignments | `POST` | `/requests/{requestId}/reassign` | FRONTEND INTEGRATION REQUIRED |
| Assignments | `POST` | `/requests/{requestId}/unassign` | FRONTEND INTEGRATION REQUIRED |
| Scheduling | `PATCH` | `/requests/{requestId}/schedule` | FRONTEND INTEGRATION REQUIRED |
| Assignment action | `POST` | `/requests/{requestId}/assignment/accept` | FRONTEND INTEGRATION REQUIRED |
| Assignment action | `POST` | `/requests/{requestId}/assignment/reject` | FRONTEND INTEGRATION REQUIRED |
| Assignment history | `GET` | `/requests/{requestId}/assignments` | FRONTEND INTEGRATION REQUIRED |
| Workload | `GET` | `/technicians/{technicianId}/workload` | FRONTEND INTEGRATION REQUIRED |
| Execution | `POST` | `/requests/{requestId}/start` | FRONTEND INTEGRATION REQUIRED |
| Execution | `PATCH` | `/requests/{requestId}/diagnosis` | FRONTEND INTEGRATION REQUIRED |
| Execution | `POST` | `/requests/{requestId}/wait-for-parts` | FRONTEND INTEGRATION REQUIRED |
| Execution | `POST` | `/requests/{requestId}/resume` | FRONTEND INTEGRATION REQUIRED |
| Execution | `POST` | `/requests/{requestId}/complete` | FRONTEND INTEGRATION REQUIRED |
| Execution | `POST` | `/requests/{requestId}/cancel` | FRONTEND INTEGRATION REQUIRED |
| Execution | `GET` | `/requests/{requestId}/execution` | FRONTEND INTEGRATION REQUIRED |
| Execution | `GET` | `/requests/{requestId}/status-history` | FRONTEND INTEGRATION REQUIRED |
| Attachments | `POST` | `/requests/{requestId}/attachments` | FRONTEND INTEGRATION REQUIRED |
| Attachments | `GET` | `/requests/{requestId}/attachments` | FRONTEND INTEGRATION REQUIRED |
| Attachments | `GET` | `/attachments/{attachmentId}` | FRONTEND INTEGRATION REQUIRED |
| Attachments | `GET` | `/attachments/{attachmentId}/download-url` | FRONTEND INTEGRATION REQUIRED |
| Attachments | `DELETE` | `/attachments/{attachmentId}` | FRONTEND INTEGRATION REQUIRED |
| Dashboard | `GET` | `/dashboard/overview` | OPTIONAL FOR MVP UI |
| Dashboard | `GET` | `/dashboard/request-trends` | OPTIONAL FOR MVP UI |
| Dashboard | `GET` | `/dashboard/requests-by-status` | OPTIONAL FOR MVP UI |
| Dashboard | `GET` | `/dashboard/requests-by-category` | OPTIONAL FOR MVP UI |
| Dashboard | `GET` | `/dashboard/technicians` | OPTIONAL FOR MVP UI |
| Dashboard | `GET` | `/dashboard/reviews` | OPTIONAL FOR MVP UI |
| Reviews | `GET` | `/reviews` | OPTIONAL FOR MVP UI |
| Reviews | `GET` | `/reviews/{reviewId}` | OPTIONAL FOR MVP UI |
| Reviews | `GET` | `/reviews/summary` | OPTIONAL FOR MVP UI |
| Notifications | `GET` | `/notifications` | OPTIONAL FOR MVP UI |
| Notifications | `GET` | `/notifications/{notificationId}` | OPTIONAL FOR MVP UI |
| Notifications | `POST` | `/notifications/{notificationId}/retry` | OPTIONAL FOR MVP UI |
| Telegram | `POST` | `/telegram/webhook` | BACKEND-ONLY |

## 29. Status Transition Matrices

### Repair Request

| Trigger | From | To |
| --- | --- | --- |
| Create staff request | none | `NEW` |
| Assign without schedule | `NEW` | `ASSIGNED` |
| Assign with schedule | `NEW` | `SCHEDULED` |
| Schedule active assignment | `ASSIGNED` or `SCHEDULED` | `SCHEDULED` |
| Clear schedule | `SCHEDULED` | `ASSIGNED` |
| Reject/unassign active assignment | `ASSIGNED` or `SCHEDULED` | `NEW` |
| Start repair | `ASSIGNED` or `SCHEDULED` | `IN_PROGRESS` |
| Wait for parts | `IN_PROGRESS` | `WAITING_FOR_PARTS` |
| Resume repair | `WAITING_FOR_PARTS` | `IN_PROGRESS` |
| Complete repair | `IN_PROGRESS` | `COMPLETED` |
| Cancel | `NEW`, `ASSIGNED`, `SCHEDULED`, `IN_PROGRESS`, `WAITING_FOR_PARTS` | `CANCELLED` |

### Assignment

| Trigger | Result |
| --- | --- |
| Assign | New active assignment with `PENDING`. |
| Accept | Active assignment becomes `ACCEPTED`. |
| Reject | Active assignment becomes `REJECTED`; request returns to `NEW`. |
| Reassign | Previous active assignment becomes `REASSIGNED`; new active assignment is created. |
| Unassign | Active assignment becomes `UNASSIGNED`; request returns to `NEW`. |
| Complete repair | Active assignment becomes `COMPLETED`. |
| Cancel request | Active assignment becomes `CANCELLED`. |

## 30. Frontend Implementation Plan

1. Shared infrastructure: base URL, typed HTTP client, bearer interceptor, single-flight refresh, error parser, trace ID capture, pagination helpers, enum translations, date utilities.
2. Auth: login, session restore through `/auth/me`, refresh, logout, logout-all, password change, role-aware routing.
3. Reference data: customers, categories, technicians, active filters, workload display.
4. Request workspace: list, filters, sorting, pagination, detail, create, update, customer history, source/status/priority display.
5. Assignment and scheduling: assign, accept, reject, reassign, unassign, schedule, reschedule, clear schedule, assignment history, workload refresh.
6. Execution: start, diagnosis, wait for parts, resume, complete, cancel, execution panel, status history.
7. Attachments: upload progress, type selection, list, download URL, delete, completion-photo indicator.
8. Telegram-originated data: show source, linked customer, Telegram photos, language, reviews, and notification state.
9. Localization: EN/RU/UZ labels for enums, action labels, validation, and backend error codes.
10. Optional operations: dashboards, reviews, notification admin, technician Telegram link.
11. End-to-end QA.

## 31. End-to-End Integration Scenarios

Must-pass scenarios:

- Login, restore session, refresh expired access token, logout.
- Repeated failed login returns throttling behavior when configured.
- Create customer, category, technician, and repair request.
- Filter/search/sort/paginate request list.
- Assign technician, schedule visit, accept assignment, inspect workload.
- Start repair, record diagnosis, wait for parts, resume.
- Upload `COMPLETION_PHOTO`, complete repair, confirm workload release.
- Attempt completion without completion photo and show `COMPLETION_PHOTO_REQUIRED`.
- Reassign or reject assignment and verify assignment history.
- Try invalid actions: inactive customer/category, overloaded technician, terminal request edit, invalid status transition.
- Upload invalid file type/oversized file and show backend error.
- Display Telegram-created request/review data without calling webhook.

## 32. Frontend Acceptance Criteria

Integration is complete when:

- All protected calls send `Authorization: Bearer <accessToken>`.
- Refresh token rotation is single-flight and atomic.
- All relevant errors are handled: `400`, `401`, `403`, `404`, `409`, `429`, `500`.
- `X-Trace-Id` is captured from every response and error.
- Buttons and routes are role-aware, but backend errors remain authoritative.
- Lists use backend pagination, sort, and filters.
- Every command refreshes affected server state.
- Terminal requests are read-only except allowed read/download actions.
- Technician capacity conflicts refresh workload and request detail.
- Attachment upload, download URL, and delete work.
- Completion evidence is visible and completion blocking is clear.
- Telegram-originated records are distinguishable and use the normal workflow.
- EN/RU/UZ labels exist for user-facing enums and common error codes.
- UTC/offset date handling is consistent with the dashboard business timezone.

## 33. Deployment and Operational Notes

Frontend deployment checklist:

- Set production API base URL.
- Ensure backend `APP_CORS_ALLOWED_ORIGINS` includes production frontend origin.
- Confirm storage is enabled before exposing attachment UI.
- Confirm Telegram is enabled before relying on Telegram-created data or notification delivery.
- Do not depend on Swagger/OpenAPI being public in production.
- Show health/readiness only in admin/ops UI if needed.

## Appendix A - Request and Response Examples

### Assign Technician

```json
{
  "technicianId": 5,
  "scheduledVisitAt": "2026-08-04T14:00:00+05:00"
}
```

### Wait For Parts

```json
{
  "reason": "Pump replacement required"
}
```

### Complete Repair

```json
{
  "workPerformed": "Replaced pump and tested spin cycle",
  "completionNote": "No leakage after final test"
}
```

### Upload Completion Photo

```text
POST /api/v1/requests/12/attachments?type=COMPLETION_PHOTO
Content-Type: multipart/form-data
file=<binary image>
```

### Download URL Response

```json
{
  "url": "https://storage.example/presigned-url",
  "expiresAt": "2026-08-03T10:10:00Z"
}
```

## Appendix B - Error Code Catalogue

Client should map these codes as stable keys:

```text
VALIDATION_FAILED
INVALID_REQUEST_BODY
INVALID_REQUEST_PARAMETER
MISSING_REQUEST_PARAMETER
METHOD_NOT_ALLOWED
UNSUPPORTED_MEDIA_TYPE
RESOURCE_NOT_FOUND
BUSINESS_RULE_VIOLATION
UNAUTHORIZED
ACCESS_DENIED
INVALID_CREDENTIALS
USER_NOT_FOUND
USER_EMAIL_ALREADY_EXISTS
CUSTOMER_NOT_FOUND
CUSTOMER_PHONE_ALREADY_EXISTS
CUSTOMER_TELEGRAM_ID_ALREADY_EXISTS
CUSTOMER_INACTIVE
TECHNICIAN_NOT_FOUND
TECHNICIAN_PHONE_ALREADY_EXISTS
TECHNICIAN_TELEGRAM_ID_ALREADY_EXISTS
TECHNICIAN_INACTIVE
INVALID_MAXIMUM_CONCURRENT_REQUESTS
CATEGORY_NOT_FOUND
CATEGORY_NAME_EN_ALREADY_EXISTS
CATEGORY_NAME_UZ_ALREADY_EXISTS
CATEGORY_NAME_RU_ALREADY_EXISTS
CATEGORY_INACTIVE
INVALID_CATEGORY_ORDER
REPAIR_REQUEST_NOT_FOUND
REPAIR_REQUEST_NOT_EDITABLE
REPAIR_REQUEST_NOT_ASSIGNABLE
REPAIR_REQUEST_ALREADY_ASSIGNED
REPAIR_REQUEST_CUSTOMER_INACTIVE
REPAIR_REQUEST_CATEGORY_INACTIVE
INVALID_REPAIR_REQUEST_DESCRIPTION
INVALID_REPAIR_REQUEST_LOCATION
INVALID_PREFERRED_VISIT_TIME
INVALID_REQUEST_DATE_RANGE
ACTIVE_ASSIGNMENT_NOT_FOUND
TECHNICIAN_CAPACITY_EXCEEDED
ASSIGNMENT_ALREADY_ACCEPTED
ASSIGNMENT_ALREADY_REJECTED
ASSIGNMENT_NOT_PENDING
INVALID_SCHEDULED_VISIT_TIME
ASSIGNMENT_CONFLICT
REQUEST_NOT_SCHEDULABLE
REPAIR_EXECUTION_NOT_FOUND
REPAIR_ALREADY_STARTED
REPAIR_NOT_STARTABLE
REPAIR_NOT_IN_PROGRESS
REPAIR_NOT_WAITING_FOR_PARTS
REPAIR_ALREADY_COMPLETED
REPAIR_ALREADY_CANCELLED
DIAGNOSIS_REQUIRED
INVALID_DIAGNOSIS
WORK_PERFORMED_REQUIRED
INVALID_WAITING_REASON
INVALID_CANCELLATION_REASON
ACTIVE_ACCEPTED_ASSIGNMENT_REQUIRED
REPAIR_EXECUTION_CONFLICT
INVALID_REPAIR_STATUS_TRANSITION
ATTACHMENT_NOT_FOUND
ATTACHMENT_EMPTY
ATTACHMENT_FILE_TOO_LARGE
ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED
ATTACHMENT_CONTENT_MISMATCH
ATTACHMENT_TYPE_NOT_ALLOWED
ATTACHMENT_UPLOAD_NOT_ALLOWED
ATTACHMENT_DELETE_NOT_ALLOWED
ATTACHMENT_LIMIT_EXCEEDED
ATTACHMENT_NOT_AVAILABLE
ATTACHMENT_STORAGE_FAILED
ATTACHMENT_CONFLICT
COMPLETION_PHOTO_REQUIRED
INVALID_PHONE_NUMBER
OPTIMISTIC_LOCK_CONFLICT
USER_DISABLED
LAST_ACTIVE_ADMIN_REQUIRED
SELF_DISABLE_NOT_ALLOWED
INVALID_CURRENT_PASSWORD
PASSWORD_REUSE_NOT_ALLOWED
PASSWORD_POLICY_VIOLATION
INVALID_ACCESS_TOKEN
ACCESS_TOKEN_EXPIRED
INVALID_REFRESH_TOKEN
REFRESH_TOKEN_EXPIRED
REFRESH_TOKEN_REVOKED
REFRESH_TOKEN_REUSE_DETECTED
AUTHENTICATION_REQUIRED
INTERNAL_ERROR
```

## Appendix C - Enum Catalogue

| Enum | Values |
| --- | --- |
| `UserRole` | `ADMIN`, `MANAGER` |
| `LanguageCode` | `EN`, `RU`, `UZ` |
| `CustomerRegistrationSource` | `ADMIN`, `TELEGRAM` |
| `RepairRequestStatus` | `NEW`, `ASSIGNED`, `SCHEDULED`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`, `CANCELLED` |
| `RepairRequestPriority` | `LOW`, `NORMAL`, `HIGH`, `URGENT` |
| `RepairRequestSource` | `ADMIN`, `TELEGRAM` |
| `AssignmentStatus` | `PENDING`, `ACCEPTED`, `REJECTED`, `UNASSIGNED`, `REASSIGNED`, `COMPLETED`, `CANCELLED` |
| `AttachmentType` | `CUSTOMER_PROBLEM_PHOTO`, `DIAGNOSIS_PHOTO`, `COMPLETION_PHOTO`, `GENERAL_DOCUMENT` |
| `AttachmentStatus` | `UPLOADING`, `AVAILABLE`, `FAILED`, `DELETED` |
| `NotificationStatus` | `PENDING`, `PROCESSING`, `RETRY_SCHEDULED`, `DELIVERED`, `SKIPPED`, `DEAD` |
| `NotificationChannel` | `TELEGRAM` |
| `NotificationRecipientType` | `CUSTOMER`, `TECHNICIAN` |
| `ReviewSource` | `TELEGRAM` |
| `DashboardPeriod` | API values `7d`, `30d` |

## Appendix D - Environment Variables

Frontend/operator variables to coordinate:

```text
SERVER_PORT
APP_CORS_ALLOWED_ORIGINS
APP_OPENAPI_ENABLED
APP_SWAGGER_UI_ENABLED
APP_JWT_ACCESS_TOKEN_TTL
APP_REFRESH_TOKEN_TTL
APP_AUTH_THROTTLE_ENABLED
APP_STORAGE_ENABLED
APP_STORAGE_DOWNLOAD_URL_TTL
APP_STORAGE_MAX_FILE_SIZE
APP_STORAGE_MAX_REQUEST_SIZE
APP_STORAGE_MAX_FILES_PER_REQUEST
APP_STORAGE_MAX_FILES_PER_TYPE
APP_TELEGRAM_ENABLED
APP_NOTIFICATION_WORKER_ENABLED
APP_DASHBOARD_BUSINESS_TIME_ZONE
```

## Appendix E - Database Migrations

Source contains Flyway migrations `V1` through `V17`, covering foundation, users, refresh sessions, auth versioning, reference data, repair requests, language support, assignments, execution lifecycle, attachments, Telegram workflows, notification outbox, reviews, dashboard indexes, and production hardening.

## Appendix F - Test and Verification Evidence

Relevant test suites present in source:

- `AuthIntegrationTest`, `AuthThrottleIntegrationTest`, `AuthVersionIntegrationTest`
- `UserManagementIntegrationTest`, `UserPaginationIntegrationTest`
- `CustomerIntegrationTest`, `TechnicianIntegrationTest`, `RepairCategoryIntegrationTest`
- `RepairRequestIntegrationTest`
- `RepairAssignmentIntegrationTest`
- `RepairExecutionIntegrationTest`
- `AttachmentIntegrationTest`, `S3ObjectStorageServiceIntegrationTest`
- `TelegramCustomerBotIntegrationTest`, `TelegramTechnicianBotIntegrationTest`
- `NotificationIntegrationTest`
- `RepairReviewIntegrationTest`
- `DashboardIntegrationTest`
- production/security/schema migration tests through later phases

This report was regenerated from source inspection. No full Gradle test run was performed as part of this documentation-only change.
