# Frontend Integration Report: Avatars and Request Image Previews

Date: 2026-08-31

## Backend Contract Now Available

The backend now exposes display-ready image metadata for:

- staff users: admins and managers;
- customers;
- technicians;
- mobile self profile users;
- repair request attachments/photos.

The important frontend change is that images no longer need to be shown only through manual download buttons. Backend DTOs now include API-relative `downloadUrl` fields that can be rendered as image sources after passing through the app's existing API/auth layer.

## Avatar Response Shape

Any DTO that contains an avatar now uses this shape:

```json
{
  "avatar": {
    "attachmentId": 123,
    "fileName": "avatar.png",
    "contentType": "image/png",
    "sizeBytes": 48291,
    "downloadUrl": "/api/v1/me/avatar",
    "uploadedAt": "2026-08-31T07:20:00Z"
  }
}
```

If no avatar exists, `avatar` is `null`.

## Avatar URL Sources

Backend may return these avatar URLs:

```text
/api/v1/me/avatar
/api/v1/users/{userId}/avatar
/api/v1/customers/{customerId}/avatar
/api/v1/technicians/{technicianId}/avatar
/api/v1/mobile/me/avatar
```

For the Nuxt web app, convert backend API-relative URLs to the local proxy path.

Examples:

```text
Backend: /api/v1/me/avatar
Web:     /api/me/avatar

Backend: /api/v1/customers/12/avatar
Web:     /api/customers/12/avatar
```

Mobile apps can call backend URLs directly with the bearer token.

## Request Attachment Response Shape

Request attachment list/detail responses now include `downloadUrl` and `imagePreview`.

```json
{
  "id": 501,
  "repairRequestId": 42,
  "type": "CUSTOMER_PROBLEM_PHOTO",
  "originalFileName": "fault.jpg",
  "contentType": "image/jpeg",
  "sizeBytes": 1834421,
  "status": "AVAILABLE",
  "uploadedBy": null,
  "uploadedByTechnician": null,
  "uploadedAt": "2026-08-31T07:20:00Z",
  "downloadUrl": "/api/v1/attachments/501/download",
  "imagePreview": true
}
```

Mobile attachment responses use:

```text
/api/v1/mobile/me/attachments/{attachmentId}/download
```

## Rendering Rules

For avatars:

- if `avatar` is present, render `avatar.downloadUrl`;
- if `avatar` is `null`, keep the existing initials/default-image fallback;
- after avatar upload, use the returned avatar DTO to update local state;
- after avatar delete, clear local avatar state.

For request attachments:

- if `imagePreview === true`, render a thumbnail or preview using `downloadUrl`;
- keep the download/open button for all attachments;
- if `imagePreview === false`, do not render an image preview; show file metadata and download/open action only.

Previewable image MIME types are:

```text
image/jpeg
image/png
image/webp
image/gif
```

## Required Nuxt Proxy Update

The web proxy must treat the new streaming paths as binary responses, not JSON.

Add binary handling for:

```text
GET /api/me/avatar
GET /api/users/{id}/avatar
GET /api/customers/{id}/avatar
GET /api/technicians/{id}/avatar
GET /api/attachments/{id}/download
```

If web ever consumes mobile routes, also handle:

```text
GET /api/mobile/me/avatar
GET /api/mobile/me/attachments/{id}/download
```

The proxy should preserve:

- `content-type`;
- `content-disposition`;
- `cache-control`.

It should return raw bytes/array buffer to the browser.

## Suggested Web Helper

Add a small helper so UI components do not manually rewrite URLs everywhere:

```ts
export function apiAssetUrl(downloadUrl?: string | null): string | undefined {
  if (!downloadUrl) return undefined
  return downloadUrl.replace(/^\/api\/v1\//, '/api/')
}
```

Usage:

```vue
<img
  v-if="user.avatar"
  :src="apiAssetUrl(user.avatar.downloadUrl)"
  :alt="user.fullName"
>
```

## Screens To Update

Update these web areas:

- topbar current-user avatar;
- profile avatar editor;
- admin users list/detail if avatars are shown there;
- customers list/detail;
- technicians list/detail;
- request detail attachment section.

Update mobile areas:

- customer profile avatar;
- technician profile avatar;
- repair request/job attachment gallery;
- upload success flows to refresh or replace the returned image metadata.

## Backward Compatibility

Existing fields remain available:

- `attachmentId`;
- `fileName`;
- `contentType`.

Frontend can integrate incrementally:

1. start with request attachment previews;
2. switch current profile avatar to `avatar.downloadUrl`;
3. add staff/customer/technician avatar display in lists/details;
4. add mobile avatar upload/display.

## Acceptance Checklist

- Current staff user avatar renders in the topbar and profile page.
- Admin can see staff avatars where user records are displayed.
- Customer avatars render in customer screens.
- Technician avatars render in technician screens.
- Mobile customer/technician profile avatar renders.
- Request photo attachments render inline thumbnails.
- PDF/general document attachments still show download/open controls only.
- Authenticated image URLs work after page refresh.
- Missing avatars fall back to initials/default UI.
