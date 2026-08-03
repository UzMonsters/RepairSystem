# Repair Requests

Phase 6 implements staff repair-request intake, technician assignment, confirmed
visit scheduling, repair execution lifecycle, and private repair attachments. It
does not implement Telegram adapters, notifications, pricing, payments,
spare-parts inventory, reviews, dashboards, or customer/technician
authentication.

## Model

`repair_requests` stores:

- Immutable public `requestNumber`
- `customerId` and `categoryId` foreign keys
- Problem `description`
- `address` or latitude/longitude pair
- `priority`
- Backend-controlled `status`
- Backend-controlled `source`
- Optional `customerPreferredVisitAt`
- Optional `internalNote`
- Backend-controlled `createdByUserId`
- UTC timestamps and optimistic-lock `version`

`repair_assignments` stores assignment history. A request may have many
historical assignment rows but at most one active assignment (`PENDING` or
`ACCEPTED`), enforced by a PostgreSQL partial unique index.

`repair_executions` stores one execution record per request. It captures start,
diagnosis, current waiting-for-parts state, completion, and cancellation data.

`repair_request_status_history` is append-only. Every committed status
transition records `fromStatus`, `toStatus`, optional reason, staff user, and
timestamp. Existing requests receive one Phase 5 backfill row for their current
status.

`repair_attachments` stores attachment metadata only. File bytes are stored in
private S3-compatible object storage. See `docs/ATTACHMENTS.md`.

Archived customers and categories remain visible on historical requests because
requests keep foreign keys and summaries are read from the related records.

## Request Number

The backend generates public numbers with PostgreSQL sequence
`repair_request_number_seq`. Format:

```text
REP-2026-000001
```

The year is derived in UTC. The numeric sequence is global and does not reset
annually, so a later year may continue from the previous number.

## Status, Priority, And Source

Status vocabulary exists for future workflow phases:

- `NEW`
- `ASSIGNED`
- `SCHEDULED`
- `IN_PROGRESS`
- `WAITING_FOR_PARTS`
- `COMPLETED`
- `CANCELLED`

REST creation always persists `NEW`. Phase 4 controls `NEW`, `ASSIGNED`, and
`SCHEDULED`. Phase 5 controls `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`,
and `CANCELLED`.

Assignment status effects:

- Assign without `scheduledVisitAt`: request becomes `ASSIGNED`
- Assign with `scheduledVisitAt`: request becomes `SCHEDULED`
- Add or update schedule: request becomes `SCHEDULED`
- Clear schedule while assignment remains active: request becomes `ASSIGNED`
- Accept assignment: request remains `ASSIGNED` or `SCHEDULED`
- Reject or unassign: request returns to `NEW`
- Reassign: previous active row becomes `REASSIGNED`, new row becomes active

Execution status effects:

- Start from `ASSIGNED` or `SCHEDULED`: request becomes `IN_PROGRESS`
- Wait for parts from `IN_PROGRESS`: request becomes `WAITING_FOR_PARTS`
- Resume from `WAITING_FOR_PARTS`: request becomes `IN_PROGRESS`
- Complete from `IN_PROGRESS`: request becomes `COMPLETED`
- Cancel from `NEW`, `ASSIGNED`, `SCHEDULED`, `IN_PROGRESS`, or
  `WAITING_FOR_PARTS`: request becomes `CANCELLED`

Terminal statuses `COMPLETED` and `CANCELLED` reject further lifecycle changes.

Priority values are `LOW`, `NORMAL`, `HIGH`, and `URGENT`. Missing create
priority defaults to `NORMAL`.

Source values are `ADMIN` and `TELEGRAM`. Phase 3 REST creation always persists
`ADMIN`; Telegram intake is deferred.

## Validation

Customer and category are required and must be active when selected for create
or update. Inactive records return `409` with
`REPAIR_REQUEST_CUSTOMER_INACTIVE` or `REPAIR_REQUEST_CATEGORY_INACTIVE`.

Description is required, trimmed, and must be 10 to 2000 characters.

Location must include either a nonblank address or both coordinates. Latitude
and longitude must be provided as a pair. Latitude must be `-90..90`; longitude
must be `-180..180`. PostgreSQL stores latitude as `NUMERIC(9,6)` and longitude
as `NUMERIC(10,6)`.

`customerPreferredVisitAt` is optional, uses ISO 8601 with timezone, is stored
consistently in UTC, and is only a customer preference.

`scheduledVisitAt` belongs to the active assignment, accepts ISO 8601 timestamps
with offsets, must be in the future, and is stored in UTC. Clearing the schedule
uses `{"clearSchedule":true}`.

`internalNote` is optional, staff-only, trimmed, and limited to 2000 characters.

Diagnosis is plain Unicode text, trimmed, required for completion, and limited
to 4000 characters. Waiting and cancellation reasons are required and limited to
1000 characters. Work performed is required on completion and limited to 4000
characters. Completion note is optional and limited to 2000 characters.

Completion also requires at least one available `COMPLETION_PHOTO`. Uploading,
failed, or deleted completion photos do not satisfy this rule.

## Editability

Only `NEW` requests are editable. Editable fields:

- `customerId`
- `categoryId`
- `description`
- `address`
- `latitude`
- `longitude`
- `priority`
- `customerPreferredVisitAt`
- `internalNote`

Immutable fields include `id`, `requestNumber`, `status`, `source`,
`createdByUserId`, `createdAt`, `updatedAt`, and `version`. Updating a non-NEW
request returns `409 REPAIR_REQUEST_NOT_EDITABLE`.

## Endpoints

All endpoints require `ADMIN` or `MANAGER`.

- `GET /api/v1/requests`: list, search, filter, sort, and paginate requests
- `GET /api/v1/requests/{id}`: get details
- `POST /api/v1/requests`: create staff request intake
- `PUT /api/v1/requests/{id}`: update editable intake fields
- `GET /api/v1/customers/{customerId}/requests`: customer repair history
- `POST /api/v1/requests/{requestId}/assign`: assign technician
- `POST /api/v1/requests/{requestId}/reassign`: reassign technician
- `POST /api/v1/requests/{requestId}/unassign`: unassign technician
- `PATCH /api/v1/requests/{requestId}/schedule`: schedule, reschedule, or clear
  confirmed visit
- `POST /api/v1/requests/{requestId}/assignment/accept`: accept active
  assignment
- `POST /api/v1/requests/{requestId}/assignment/reject`: reject active
  assignment
- `GET /api/v1/requests/{requestId}/assignments`: assignment history, newest
  first
- `POST /api/v1/requests/{requestId}/start`: start accepted assigned repair
- `PATCH /api/v1/requests/{requestId}/diagnosis`: create or update diagnosis
- `POST /api/v1/requests/{requestId}/wait-for-parts`: move to waiting
- `POST /api/v1/requests/{requestId}/resume`: resume from waiting
- `POST /api/v1/requests/{requestId}/complete`: complete repair
- `POST /api/v1/requests/{requestId}/cancel`: cancel request
- `GET /api/v1/requests/{requestId}/execution`: execution details
- `GET /api/v1/requests/{requestId}/status-history`: append-only status
  history, newest first
- `POST /api/v1/requests/{requestId}/attachments`: upload attachment
- `GET /api/v1/requests/{requestId}/attachments`: list available attachments
- `GET /api/v1/attachments/{attachmentId}`: attachment metadata
- `GET /api/v1/attachments/{attachmentId}/download-url`: short-lived download
  URL
- `DELETE /api/v1/attachments/{attachmentId}`: soft-delete available attachment

There is no `DELETE /api/v1/requests/{id}` endpoint.

## Query Behavior

Request search covers request number, customer full name, normalized customer
phone, description, address, English category name, Russian category name, and
Uzbek category name.
Search is bounded to 120 characters.

Filters:

- `requestNumber`
- `customerId`
- `categoryId`
- `status`
- `priority`
- `source`
- `createdFrom`
- `createdTo`
- `preferredVisitFrom`
- `preferredVisitTo`

Customer history supports `status`, `priority`, `categoryId`, `createdFrom`,
and `createdTo`.

Allowed sort fields are `id`, `requestNumber`, `priority`, `status`, `source`,
`customerPreferredVisitAt`, `createdAt`, `updatedAt`, `customerName`, and
`categoryName`. Page size is `1..100`.

Date ranges where `from` is later than `to` return
`400 INVALID_REQUEST_DATE_RANGE`.

## Errors

Stable Phase 3 codes:

- `REPAIR_REQUEST_NOT_FOUND`
- `REPAIR_REQUEST_NOT_EDITABLE`
- `REPAIR_REQUEST_CUSTOMER_INACTIVE`
- `REPAIR_REQUEST_CATEGORY_INACTIVE`
- `INVALID_REPAIR_REQUEST_DESCRIPTION`
- `INVALID_REPAIR_REQUEST_LOCATION`
- `INVALID_PREFERRED_VISIT_TIME`
- `INVALID_REQUEST_DATE_RANGE`

Stable Phase 4 assignment/scheduling codes:

- `REPAIR_REQUEST_NOT_ASSIGNABLE`
- `REPAIR_REQUEST_ALREADY_ASSIGNED`
- `ACTIVE_ASSIGNMENT_NOT_FOUND`
- `TECHNICIAN_NOT_FOUND`
- `TECHNICIAN_INACTIVE`
- `TECHNICIAN_CAPACITY_EXCEEDED`
- `ASSIGNMENT_ALREADY_ACCEPTED`
- `ASSIGNMENT_ALREADY_REJECTED`
- `ASSIGNMENT_NOT_PENDING`
- `INVALID_SCHEDULED_VISIT_TIME`
- `ASSIGNMENT_CONFLICT`
- `REQUEST_NOT_SCHEDULABLE`

Stable Phase 5 lifecycle codes:

- `REPAIR_EXECUTION_NOT_FOUND`
- `REPAIR_ALREADY_STARTED`
- `REPAIR_NOT_STARTABLE`
- `REPAIR_NOT_IN_PROGRESS`
- `REPAIR_NOT_WAITING_FOR_PARTS`
- `REPAIR_ALREADY_COMPLETED`
- `REPAIR_ALREADY_CANCELLED`
- `DIAGNOSIS_REQUIRED`
- `INVALID_DIAGNOSIS`
- `WORK_PERFORMED_REQUIRED`
- `INVALID_WAITING_REASON`
- `INVALID_CANCELLATION_REASON`
- `ACTIVE_ACCEPTED_ASSIGNMENT_REQUIRED`
- `REPAIR_EXECUTION_CONFLICT`
- `INVALID_REPAIR_STATUS_TRANSITION`

Phase 2 codes are reused where appropriate: `CUSTOMER_NOT_FOUND` and
`CATEGORY_NOT_FOUND`.

## Tests

Phase 5 adds PostgreSQL integration tests for start, diagnosis, wait/resume,
completion, cancellation, terminal-state protection, assignment closeout,
workload release, execution details, status history, security, schema objects,
and lifecycle concurrency invariants.
