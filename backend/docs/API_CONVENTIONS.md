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

Authentication and authorization use stable codes such as
`AUTHENTICATION_REQUIRED`, `INVALID_CREDENTIALS`, `INVALID_ACCESS_TOKEN`,
`ACCESS_TOKEN_EXPIRED`, `INVALID_REFRESH_TOKEN`, `REFRESH_TOKEN_REVOKED`,
`REFRESH_TOKEN_EXPIRED`, `REFRESH_TOKEN_REUSE_DETECTED`, and `ACCESS_DENIED`.

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
