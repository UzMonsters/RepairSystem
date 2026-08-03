# Attachments

Phase 6 stores repair-file metadata in PostgreSQL and file bytes in a private
S3-compatible bucket. Local Docker uses MinIO. Production can use AWS S3,
Cloudflare R2, DigitalOcean Spaces, or another S3-compatible provider.

## Storage Model

The API streams multipart uploads through the backend into object storage.
PostgreSQL stores only metadata: request ownership, attachment type, status,
safe generated storage key, original filename, detected content type, size,
SHA-256 checksum, uploader, soft-delete fields, failure reason, timestamps, and
version.

Business code depends on `ObjectStorageService`. MinIO/S3-specific code stays in
the attachment infrastructure package.

Buckets must remain private. API responses do not expose storage keys, bucket
names, credentials, checksums, or provider URLs. Download access is granted only
after backend authorization through a short-lived presigned URL.

## Configuration

Important variables:

- `APP_STORAGE_ENABLED`
- `APP_STORAGE_ENDPOINT`
- `APP_STORAGE_REGION`
- `APP_STORAGE_BUCKET`
- `APP_STORAGE_ACCESS_KEY`
- `APP_STORAGE_SECRET_KEY`
- `APP_STORAGE_PATH_STYLE`
- `APP_STORAGE_CREATE_BUCKET`
- `APP_STORAGE_DOWNLOAD_URL_TTL`
- `APP_STORAGE_MAX_FILE_SIZE`
- `APP_STORAGE_MAX_REQUEST_SIZE`
- `APP_STORAGE_MAX_FILES_PER_REQUEST`
- `APP_STORAGE_MAX_FILES_PER_TYPE`

Docker Compose enables MinIO, path-style S3 access, and idempotent bucket
creation for local development. Production should normally set
`APP_STORAGE_CREATE_BUCKET=false` and provision the private bucket outside the
application.

Default limits: `10MB` per file, 30 files per request, 10 files per type, and a
`PT10M` download URL lifetime bounded to `PT30S..PT1H`.

## Attachment Types

- `CUSTOMER_PROBLEM_PHOTO`: allowed while request is `NEW`, `ASSIGNED`,
  `SCHEDULED`, `IN_PROGRESS`, or `WAITING_FOR_PARTS`.
- `DIAGNOSIS_PHOTO`: allowed only in `IN_PROGRESS` or `WAITING_FOR_PARTS`.
- `COMPLETION_PHOTO`: allowed only in `IN_PROGRESS`.
- `GENERAL_DOCUMENT`: allowed while request is not terminal.

No upload is allowed once a request is `COMPLETED` or `CANCELLED`.

## Supported Formats

Photo types allow `image/jpeg`, `image/png`, and `image/webp`.
General documents allow `application/pdf`, `image/jpeg`, `image/png`, and
`image/webp`.

SVG, GIF, HTML, and arbitrary office files are not supported in Phase 6.
Signatures are detected from magic bytes. Filename extensions and multipart
content type are not trusted.

## Upload Consistency

1. Reserve metadata in a short transaction: lock request, validate lifecycle and
   count limits, generate a safe key, and save an `UPLOADING` row.
2. Stream the object outside the DB transaction: detect signature, calculate
   SHA-256 while streaming, and upload to storage.
3. Finalize metadata in a new transaction: lock attachment and request, reject
   terminal requests, and mark the row `AVAILABLE`.

Failures mark the metadata row `FAILED`, store a safe bounded failure category,
and attempt best-effort object deletion. Failed rows are not returned by ordinary
business APIs.

## Completion Requirement

Completion requires at least one `AVAILABLE` `COMPLETION_PHOTO`. Uploading,
failed, or deleted photos do not count. Existing requests completed before
Phase 6 remain readable and are not retroactively invalidated.

Completion and deletion lock the request consistently. Completion photos cannot
be deleted after the request becomes `COMPLETED`.

## Endpoints

- `POST /api/v1/requests/{requestId}/attachments`: multipart upload with `type`
  and `file`.
- `GET /api/v1/requests/{requestId}/attachments`: list available attachments,
  newest first; optional `type` filter.
- `GET /api/v1/attachments/{attachmentId}`: get available attachment metadata.
- `GET /api/v1/attachments/{attachmentId}/download-url`: create a short-lived
  authorized download URL.
- `DELETE /api/v1/attachments/{attachmentId}`: soft-delete an available
  attachment before terminal request status.

All endpoints require `ADMIN` or `MANAGER`.

## Error Codes

Stable Phase 6 codes include `ATTACHMENT_NOT_FOUND`, `ATTACHMENT_EMPTY`,
`ATTACHMENT_FILE_TOO_LARGE`, `ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED`,
`ATTACHMENT_CONTENT_MISMATCH`, `ATTACHMENT_TYPE_NOT_ALLOWED`,
`ATTACHMENT_UPLOAD_NOT_ALLOWED`, `ATTACHMENT_DELETE_NOT_ALLOWED`,
`ATTACHMENT_LIMIT_EXCEEDED`, `ATTACHMENT_NOT_AVAILABLE`,
`ATTACHMENT_STORAGE_FAILED`, `ATTACHMENT_CONFLICT`, and
`COMPLETION_PHOTO_REQUIRED`.

Provider exceptions, bucket names, storage keys, credentials, and presigned URLs
are not included in error bodies.

## Deferred

Direct presigned uploads, multipart S3 upload sessions, public files, Telegram
uploads, antivirus scanning, moderation, image resizing, thumbnails, EXIF
stripping, OCR, file versioning, object lifecycle policies, orphan cleanup,
notifications, reviews, dashboards, pricing, payments, and spare-parts inventory
remain out of scope.
