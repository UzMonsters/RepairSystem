# Technicians

Technicians are Phase 2 reference data. They do not receive system accounts or
login credentials in this phase.

## Fields

- `fullName`: required, max 160 characters
- `phone`: required, normalized to `+998XXXXXXXXX`
- `specialization`: optional, max 120 characters
- `notes`: optional, max 1000 characters
- `maximumConcurrentRequests`: positive integer, defaults to 5
- `preferredLanguage`: `EN`, `RU`, or `UZ`; manual creation defaults to `UZ`
- `active`: deactivation/reactivation flag
- `telegramLinked`: response-only boolean derived from nullable Telegram fields

Raw Telegram identifiers, `telegramLinkedAt`, audit fields, and
optimistic-lock versions are not writable through ordinary technician APIs and
are not exposed in technician DTOs.

## Endpoints

All endpoints require `ADMIN` or `MANAGER`.

- `GET /api/v1/technicians`
- `GET /api/v1/technicians/{id}`
- `POST /api/v1/technicians`
- `PUT /api/v1/technicians/{id}`
- `PATCH /api/v1/technicians/{id}/activation`
- `GET /api/v1/technicians/{id}/workload`

There is no hard-delete endpoint.

`GET /api/v1/technicians/{id}/workload` returns active assignment counts for
`PENDING` and `ACCEPTED` assignments, remaining capacity, and availability.
Completed or cancelled assignments are closed and no longer count toward active
workload.

## Filters And Sorting

Filters: `search`, `phone`, `specialization`, `active`, `telegramLinked`.

Allowed sort fields: `id`, `fullName`, `phone`, `specialization`,
`maximumConcurrentRequests`, `active`, `createdAt`, `updatedAt`.
