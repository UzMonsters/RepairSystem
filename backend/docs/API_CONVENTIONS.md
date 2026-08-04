# API Conventions

## Versioning

Application APIs use the `/api/v1` prefix.

## JSON

JSON fields use lower camel case.

## Date And Time

API timestamps use ISO 8601. Database timestamps are stored in UTC.

## Pagination

Request convention:

```text
page=0
size=20
sort=createdAt,desc
```

For list endpoints, pagination is validated explicitly:

- Default page: `0`
- Default size: `20`
- Minimum size: `1`
- Maximum size: `100`
- Negative page, zero/negative size, size greater than `100`, malformed sort,
  unsupported sort fields, and invalid directions return `400` with
  `INVALID_REQUEST_PARAMETER`.

Allowed user sort fields:

- `id`
- `fullName`
- `email`
- `role`
- `active`
- `createdAt`
- `updatedAt`
- `lastLoginAt`

Sensitive/internal fields such as `passwordHash`, `passwordChangedAt`,
`authVersion`, and optimistic-lock `version` are not public sort fields.

Allowed customer sort fields: `id`, `fullName`, `phone`, `preferredLanguage`,
`registrationSource`, `active`, `createdAt`, `updatedAt`.

Allowed technician sort fields: `id`, `fullName`, `phone`, `specialization`,
`maximumConcurrentRequests`, `active`, `createdAt`, `updatedAt`.

Allowed category sort fields: `id`, `nameEn`, `nameRu`, `nameUz`, `active`,
`displayOrder`, `createdAt`, `updatedAt`. The default category sort is
`displayOrder,asc`.

Allowed repair-request sort fields: `id`, `requestNumber`, `priority`,
`status`, `source`, `customerPreferredVisitAt`, `createdAt`, `updatedAt`,
`customerName`, and `categoryName`. Request page size is also bounded to
`1..100`.

Response convention:

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

## Errors

All API errors use this shape:

```json
{
  "timestamp": "2026-07-31T06:00:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "path": "/api/v1/example",
  "traceId": "4f8c9348c3a7",
  "fieldErrors": [
    {
      "field": "exampleField",
      "code": "NotBlank",
      "message": "Example field is required."
    }
  ]
}
```

Business-rule failures must preserve stable business error codes. Unexpected
exceptions return `INTERNAL_ERROR` and do not expose stack traces.

Invalid page/sort parameters use `INVALID_REQUEST_PARAMETER`, include a safe
field error for the public request parameter, and must not expose
`PropertyReferenceException` or JPA property-resolution details.

Authentication and authorization use stable codes such as
`AUTHENTICATION_REQUIRED`, `INVALID_CREDENTIALS`, `INVALID_ACCESS_TOKEN`,
`ACCESS_TOKEN_EXPIRED`, `INVALID_REFRESH_TOKEN`, `REFRESH_TOKEN_REVOKED`,
`REFRESH_TOKEN_EXPIRED`, `REFRESH_TOKEN_REUSE_DETECTED`, and `ACCESS_DENIED`.

Reference-data business errors use stable codes including
`CUSTOMER_NOT_FOUND`, `CUSTOMER_PHONE_ALREADY_EXISTS`, `TECHNICIAN_NOT_FOUND`,
`TECHNICIAN_PHONE_ALREADY_EXISTS`, `CATEGORY_NOT_FOUND`,
`CATEGORY_NAME_EN_ALREADY_EXISTS`, `CATEGORY_NAME_UZ_ALREADY_EXISTS`,
`CATEGORY_NAME_RU_ALREADY_EXISTS`, `INVALID_CATEGORY_ORDER`,
`INVALID_PHONE_NUMBER`, and
`OPTIMISTIC_LOCK_CONFLICT`.

Repair-request intake errors use stable codes including
`REPAIR_REQUEST_NOT_FOUND`, `REPAIR_REQUEST_NOT_EDITABLE`,
`REPAIR_REQUEST_CUSTOMER_INACTIVE`, `REPAIR_REQUEST_CATEGORY_INACTIVE`,
`INVALID_REPAIR_REQUEST_DESCRIPTION`, `INVALID_REPAIR_REQUEST_LOCATION`,
`INVALID_PREFERRED_VISIT_TIME`, and `INVALID_REQUEST_DATE_RANGE`.

## Authentication

Protected APIs use:

```text
Authorization: Bearer <accessToken>
```

Access tokens are JWTs. Refresh tokens are opaque one-time tokens. The backend
rotates refresh tokens on every successful refresh and stores only token hashes.

Public auth endpoints:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

Authenticated auth endpoints:

- `GET /api/v1/auth/me`
- `PATCH /api/v1/auth/password`
- `POST /api/v1/auth/logout-all`

Admin user-management endpoints:

- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `POST /api/v1/users`
- `PUT /api/v1/users/{id}`
- `PATCH /api/v1/users/{id}/role`
- `PATCH /api/v1/users/{id}/activation`
- `POST /api/v1/users/{id}/revoke-sessions`

Customer endpoints, requiring `ADMIN` or `MANAGER`:

- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `POST /api/v1/customers`
- `PUT /api/v1/customers/{id}`
- `PATCH /api/v1/customers/{id}/activation`

Technician endpoints, requiring `ADMIN` or `MANAGER`:

- `GET /api/v1/technicians`
- `GET /api/v1/technicians/{id}`
- `POST /api/v1/technicians`
- `PUT /api/v1/technicians/{id}`
- `PATCH /api/v1/technicians/{id}/activation`

Category read endpoints, requiring `ADMIN` or `MANAGER`:

- `GET /api/v1/categories`
- `GET /api/v1/categories/{id}`

Category management endpoints, requiring `ADMIN`:

- `POST /api/v1/categories`
- `PUT /api/v1/categories/{id}`
- `PATCH /api/v1/categories/{id}/activation`
- `PATCH /api/v1/categories/reorder`

Repair-request endpoints, requiring `ADMIN` or `MANAGER`:

- `GET /api/v1/requests`
- `GET /api/v1/requests/{id}`
- `POST /api/v1/requests`
- `PUT /api/v1/requests/{id}`
- `GET /api/v1/customers/{customerId}/requests`
- `POST /api/v1/requests/{requestId}/assign`
- `POST /api/v1/requests/{requestId}/reassign`
- `POST /api/v1/requests/{requestId}/unassign`
- `PATCH /api/v1/requests/{requestId}/schedule`
- `POST /api/v1/requests/{requestId}/assignment/accept`
- `POST /api/v1/requests/{requestId}/assignment/reject`
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

There is no repair-request delete endpoint. Phase 5 lifecycle control covers
the allowed request statuses from `NEW` through terminal `COMPLETED` and
`CANCELLED`. Diagnosis, waiting reasons, resume notes, work performed, and
cancellation reasons are plain Unicode text suitable for EN, RU, and UZ content.

Phase 6 attachment APIs stream multipart uploads through the backend. List and
detail responses expose business metadata only and never return storage keys,
bucket names, credentials, checksums, or permanent provider URLs. Download URLs
are generated only by `GET /api/v1/attachments/{attachmentId}/download-url`
after backend authorization and are short-lived.

## Languages

The backend uses one shared language enum for customer and technician
preferences and future localized backend messages:

- `EN`
- `RU`
- `UZ`

Manual staff-created customers and technicians default to `UZ` when the create
payload omits `preferredLanguage`. Repair categories store all three
translations as `nameEn`, `nameRu`, `nameUz` and optional `descriptionEn`,
`descriptionRu`, `descriptionUz`.

## Phone Numbers

Phase 2 supports Uzbekistan MVP phone numbers only. Values are stored as
`+998XXXXXXXXX`. Accepted inputs include:

- `+998 90 123 45 67`
- `998901234567`
- `90 123 45 67`

Structurally invalid values return `400 INVALID_PHONE_NUMBER`.

## Trace IDs

Header: `X-Trace-Id`

Valid incoming trace IDs are 8 to 64 characters and may contain letters,
numbers, periods, underscores, and hyphens. Invalid or missing values are
replaced with a generated trace ID.

Blank values, values longer than 64 characters, line breaks, control
characters, and characters outside `[A-Za-z0-9._-]` are replaced rather than
echoed to logs or response headers.

## OpenAPI

Local/test OpenAPI paths:

- `/v3/api-docs`
- `/swagger-ui.html`

Production exposure is disabled by default and can be enabled with
`APP_OPENAPI_ENABLED` and `APP_SWAGGER_UI_ENABLED`.

Password request fields are documented as `format: password` and
`writeOnly: true`.
