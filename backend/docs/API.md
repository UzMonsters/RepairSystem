# RepairAuto Backend API

RepairAuto exposes versioned JSON APIs under `/api/v1`.

## Public Endpoints

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/telegram/webhook`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/info`

All business APIs require a stateless bearer JWT. `ADMIN` and `MANAGER` can use customer, technician, request, assignment, execution, attachment, review, notification-inspection, and dashboard APIs unless a narrower endpoint rule is documented.

## Error Contract

Errors return:

- `timestamp`
- `status`
- `code`
- `message`
- `path`
- `traceId`
- `fieldErrors`

The `X-Trace-Id` response header matches `traceId` in the body. Production errors use safe messages and do not expose stack traces, SQL, filesystem paths, storage provider details, Telegram provider bodies, tokens, or secrets.

## Pagination And Sorting

List endpoints use bounded page sizes and whitelisted sort fields. Unsupported sort fields return controlled `400` errors.

## Dashboard APIs

- `GET /api/v1/dashboard/overview`
- `GET /api/v1/dashboard/request-trends?period=7d|30d`
- `GET /api/v1/dashboard/requests-by-status`
- `GET /api/v1/dashboard/requests-by-category?period=7d|30d&limit=1..20`
- `GET /api/v1/dashboard/technicians`
- `GET /api/v1/dashboard/reviews`

Dashboard timestamps are stored in UTC and grouped by the configured business timezone.
