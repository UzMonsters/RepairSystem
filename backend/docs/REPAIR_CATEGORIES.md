# Repair Categories

Repair categories are Phase 2 reference data used later by repair-request
intake and Telegram display flows.

## Fields

- `nameEn`: required, max 120 characters
- `nameRu`: required, max 120 characters
- `nameUz`: required, max 120 characters
- `descriptionEn`: optional, max 500 characters
- `descriptionRu`: optional, max 500 characters
- `descriptionUz`: optional, max 500 characters
- `displayOrder`: non-negative integer
- `active`: archive/reactivation flag

Name uniqueness is enforced with normalized case-insensitive comparison fields
stored in PostgreSQL. Those normalized fields and optimistic-lock versions are
not exposed through DTOs.

No seed categories are added in Phase 2 because English, Russian, and Uzbek
business labels need project approval.

## Permissions

`ADMIN` and `MANAGER` may read:

- `GET /api/v1/categories`
- `GET /api/v1/categories/{id}`

Only `ADMIN` may manage:

- `POST /api/v1/categories`
- `PUT /api/v1/categories/{id}`
- `PATCH /api/v1/categories/{id}/activation`
- `PATCH /api/v1/categories/reorder`

There is no hard-delete endpoint.

## Filters And Sorting

Filters: `search`, `active`.

Allowed sort fields: `id`, `nameEn`, `nameRu`, `nameUz`, `active`,
`displayOrder`, `createdAt`, `updatedAt`.

Default sort: `displayOrder,asc`.
