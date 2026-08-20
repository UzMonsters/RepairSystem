# Frontend Impact Report: Repair Request Soft Delete

## Status
**Frontend integration required.**

The backend now supports soft deletion for repair requests. Deleted requests remain in the database for audit/history integrity, but they are hidden from normal request list and detail flows.

## Backend Contract

### Soft delete request

```http
DELETE /api/v1/requests/{id}
DELETE /api/v1/repair-requests/{id}
```

### Auth
- Requires authenticated `ADMIN` or `MANAGER`.
- Uses the same bearer-token auth as the existing request endpoints.

### Success Response
```http
204 No Content
```

No response body is returned.

### Error Responses
- `401` if unauthenticated.
- `403` if the user is not allowed.
- `404` with code `REPAIR_REQUEST_NOT_FOUND` if the request does not exist.

## List Behavior

The backend now excludes soft-deleted requests by default from:
- `GET /api/v1/requests`
- `GET /api/v1/repair-requests`
- `GET /api/v1/customers/{customerId}/requests`
- Mobile customer request lists.

No extra query parameter is needed. The frontend should treat a successful delete as removal from active lists and refresh the current list/page.

## Detail Behavior

After soft delete:
- `GET /api/v1/requests/{id}` returns `404 REPAIR_REQUEST_NOT_FOUND`.
- Customer/mobile request detail access also behaves as not found.
- Update and workflow actions should not be available after deletion because the request is no longer visible through normal read paths.

## Recommended Frontend UX

Add a destructive action in the request row/detail actions menu:
- Label: `Delete` or `Hide from list`.
- Confirm before calling the endpoint.
- On `204`, refresh the list or navigate back to `/requests` from a detail page.
- On `404`, show a friendly “Request was already deleted or no longer exists” message and refresh.

This is not the same as `Cancel`. Use:
- `Cancel` for business workflow status changes.
- `Delete` for hiding duplicate/test/bad records from active lists.

## Backend Verification

Verified with:
- `gradlew testClasses`
- `RepairResourceAccessPolicyTest`
- `CustomerRepairRequestFacadeTest`
- `RepairRequestIntegrationTest` with Docker/Testcontainers/Postgres

## Changed Backend Files

- `backend/src/main/java/com/example/darks/repair_auto/repair/request/api/RepairRequestController.java`
- `backend/src/main/java/com/example/darks/repair_auto/repair/request/application/RepairRequestService.java`
- `backend/src/main/java/com/example/darks/repair_auto/repair/request/domain/RepairRequest.java`
- `backend/src/main/java/com/example/darks/repair_auto/repair/request/infrastructure/RepairRequestRepository.java`
- `backend/src/main/java/com/example/darks/repair_auto/repair/request/mobile/application/CustomerRepairRequestFacade.java`
- `backend/src/main/resources/db/migration/V34__soft_delete_repair_requests.sql`
