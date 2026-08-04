# Customers

Customers are Phase 2 reference data used later by repair-request intake.

## Fields

- `fullName`: required, max 160 characters
- `phone`: required for admin-created customers, normalized to `+998XXXXXXXXX`
- `preferredLanguage`: `EN`, `RU`, or `UZ`; manual creation defaults to `UZ`
- `registrationSource`: set by the backend, `ADMIN` in Phase 2
- `active`: archive/reactivation flag
- `telegramLinked`: response-only boolean derived from nullable Telegram fields

Raw Telegram identifiers, audit fields, and optimistic-lock versions are not
writable through customer APIs and are not exposed in customer DTOs.

## Phone Normalization

Accepted Uzbekistan MVP forms:

- `+998 90 123 45 67`
- `998901234567`
- `90 123 45 67`

Invalid values return `400 INVALID_PHONE_NUMBER`. Normalized phone numbers are
unique in PostgreSQL.

## Endpoints

All endpoints require `ADMIN` or `MANAGER`.

- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `POST /api/v1/customers`
- `PUT /api/v1/customers/{id}`
- `PATCH /api/v1/customers/{id}/activation`

There is no hard-delete endpoint.

## Filters And Sorting

Filters: `search`, `phone`, `language`, `active`, `registrationSource`,
`createdFrom`, `createdTo`.

Allowed sort fields: `id`, `fullName`, `phone`, `preferredLanguage`,
`registrationSource`, `active`, `createdAt`, `updatedAt`.
