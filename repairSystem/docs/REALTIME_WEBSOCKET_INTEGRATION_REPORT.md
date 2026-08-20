# RepairAuto Admin/Manager Web Frontend: Realtime WebSocket & Chat Integration Report

---

## 1. Current Frontend Architecture

The RepairAuto Admin/Manager frontend is built with **Nuxt 4 / Vue 3**, styled with **AdminLTE 4.1.0** and **Bootstrap 5.3**.

### Core Frontend Stack & Mechanics
- **Framework**: Nuxt 4 (`nuxt: ^4.5.1`) with Vue 3 Composition API (`<script setup lang="ts">`).
- **Data Fetching & Caching**: Nuxt's built-in `useAsyncData` with unique query keys (e.g., `'requests-list'`, `'request-${id}'`, `'dashboard'`, `'dashboard-recent'`) and `refreshNuxtData` for on-demand refetching/invalidation.
- **HTTP Client**: [`apiFetch`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/utils/api.ts) wrapped around `$fetch`, communicating through the Nuxt server proxy at [`/api/[...path].ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/server/routes/api/[...path].ts) which forwards requests to the Spring Boot backend (`${config.backendUrl}/api/v1/${path}`).
- **Authentication & Token Storage**: [`useAuth.ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/composables/useAuth.ts) stores `access_token` and `refresh_token` in cookies (`useCookie`). Session user metadata is stored in `useState<AuthUser | null>('auth:user')`.
- **Token Refresh Flow**: `refreshSession()` in [`useAuth.ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/composables/useAuth.ts) calls `/api/auth/refresh` (forwarded to backend `/api/v1/auth/refresh`), automatically invoked by `apiFetch` upon encountering HTTP 401.
- **State Management**: Vue reactivity (`ref`, `computed`, `reactive`) and Nuxt SSR-safe `useState`.
- **Localization**: [`useLocale.ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/composables/useLocale.ts) supporting `'uz'`, `'ru'`, and `'en'`. Realtime events carry machine-readable enums that are translated on the frontend via `t('status.' + status)`.
- **Error Normalization**: [`getApiErrorMessage`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/utils/api.ts) extracts error payloads, field errors, and business error codes.

---

## 2. Backend Realtime Contract Overview

The RepairAuto backend uses **Spring STOMP over WebSocket**. PostgreSQL + REST remains the authoritative source of truth; WebSockets provide instant push notifications, query invalidation signals, and chat synchronization.

- **WebSocket Endpoint**: `/ws` (supports native STOMP WebSocket and SockJS fallback).
- **Inbound Application Prefix**: `/app`
- **User-Specific Destination Prefix**: `/user`
- **Broker Prefixes**: `/queue`, `/topic`
- **Event Ordering & Durability**: Durable domain events are dispatched via `@TransactionalEventListener(phase = AFTER_COMMIT)` after the database transaction commits.

---

## 3. Connection & Authentication

### STOMP `CONNECT` Protocol
Authentication is executed at the WebSocket layer via standard STOMP `CONNECT` headers using the JWT access token:

```text
CONNECT
Authorization: Bearer <JWT_ACCESS_TOKEN>
accept-version: 1.2
```

- `authorization` is accepted case-insensitively.
- **Security Rule**: Tokens must **never** be passed in the WebSocket URL (e.g., `ws://host/ws?token=...` is prohibited).
- **Token Source**: The frontend obtains the active token from `useCookie('access_token').value` in `useAuth()`.

---

## 4. JWT Refresh & Reconnection Lifecycle

### Audit Finding: WebSocket Token Lifetime
The backend validates JWT authentication **only during the initial STOMP `CONNECT` frame**. The authenticated principal (`AuthenticatedUser`) is attached to the WebSocket session. If the token expires mid-connection, the STOMP connection remains open and authorized until disconnected.

### Required Frontend Reconnect Workflow
Whenever the frontend refreshes its JWT access token:

```text
401 encountered / Scheduled Token Refresh
                    ↓
useAuth().refreshSession() succeeds
                    ↓
new access_token stored in cookie
                    ↓
realtimeService.reconnectWithNewToken(newToken)
   1. Disconnect existing STOMP client
   2. Establish new STOMP connection with new Authorization header
   3. Resubscribe to active channels (/topic/staff.events, /user/queue/chat, etc.)
   4. Trigger REST catch-up (refreshNuxtData)
```

This must be integrated directly into [`useAuth.ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/composables/useAuth.ts) and [`api.ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/utils/api.ts).

---

## 5. Subscription Contract for Admin/Manager

The Admin/Manager web frontend must subscribe to three distinct channels upon successful STOMP connection:

| Destination | Permitted Roles | Content & Scope |
| :--- | :--- | :--- |
| **`/topic/staff.events`** | `ROLE_ADMIN`, `ROLE_MANAGER` | Staff-wide request lifecycle and dashboard invalidation events (`REQUEST_CREATED`, `REQUEST_UPDATED`, `REQUEST_ASSIGNED`, `REQUEST_UNASSIGNED`, `REQUEST_STATUS_CHANGED`, `DASHBOARD_INVALIDATED`). |
| **`/user/queue/events`** | Authenticated Staff | Staff user-scoped domain and notification events. *(No chat events).* |
| **`/user/queue/chat`** | Authenticated Staff | Realtime chat events exclusively (`CHAT_MESSAGE_CREATED`, `CHAT_MESSAGE_READ`, `CHAT_TYPING_STARTED`, `CHAT_TYPING_STOPPED`). |

> **Note**: Direct subscriptions to `/topic/...` (other than `/topic/staff.events`) or `/queue/...` without the `/user` prefix are rejected by the backend security interceptor.

---

## 6. Event Envelope & Type Discrimination

Every message received over STOMP conforms to the standard envelope:

```json
{
  "eventId": "3c90c3ae-010e-473d-9be2-5e6fa9a823b1",
  "type": "REQUEST_CREATED",
  "occurredAt": "2026-08-20T07:15:30Z",
  "payload": {}
}
```

The frontend switches on `event.type` (`RealtimeEventType`).

---

## 7. Request List Realtime Integration

### Problem Solved
When a customer submits a repair request (via Telegram bot or API), Admin/Manager staff previously had to manually reload the page.

### Realtime Reaction
Upon receiving any of the following events on `/topic/staff.events` or `/user/queue/events`:
- `REQUEST_CREATED`
- `REQUEST_UPDATED`
- `REQUEST_ASSIGNED`
- `REQUEST_UNASSIGNED`
- `REQUEST_STATUS_CHANGED`

### Payload Structure
```json
{
  "requestId": 105,
  "requestNumber": "REP-2026-000105",
  "customerId": 42,
  "technicianId": 18,
  "status": "ASSIGNED",
  "priority": "NORMAL"
}
```

### Action
Trigger Nuxt query invalidation:
```typescript
// Refetches the active paginated/filtered request list and recent requests on dashboard
refreshNuxtData('requests-list')
refreshNuxtData('dashboard-recent')
```
*Do not perform manual client-side array splicing on complex paginated/sorted tables; invoking `refreshNuxtData` guarantees data consistency.*

---

## 8. Request Detail Realtime Integration

On the Request Detail page ([`requests/[id].vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/pages/requests/[id].vue)):
If `event.payload.requestId === currentRouteId`:

```typescript
refreshNuxtData(`request-${id}`)
refreshNuxtData(`request-${id}-assignments`)
refreshNuxtData(`request-${id}-attachments`)
refreshNuxtData(`request-${id}-execution`)
refreshNuxtData(`request-${id}-status-history`)
```

This instantly updates the displayed status, assignment badges, diagnosis details, and execution logs in real time.

---

## 9. Dashboard Realtime Integration

The backend emits `DASHBOARD_INVALIDATED` to `/topic/staff.events`:

```json
{
  "reason": "REQUEST_STATUS_CHANGED"
}
```

### Invalidation Strategy
1. The WebSocket event is a lightweight notification signal and **does not** contain the full dashboard statistics object.
2. The frontend must **debounce** requests (recommended: 300ms–500ms) to avoid request storms during bursts of activity.
3. Invalidate and refetch:
```typescript
refreshNuxtData('dashboard')
refreshNuxtData('dashboard-recent')
```

---

## 10. Notification Realtime Integration

- **Backend Event Capability**: `NOTIFICATION_CREATED`, `NOTIFICATION_READ` exist in `RealtimeEventType` and are delivered over `/user/queue/events`.
- **Nuance for Staff**: In the current backend, automated system notifications primarily target Customers and Technicians. Staff inboxes ([`NavNotifications.vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/components/NavNotifications.vue)) poll or fetch `/api/v1/notifications`.
- **Handling**: If a `NOTIFICATION_CREATED` event is delivered to `/user/queue/events`, the frontend refetches the topbar notification count and `/notifications` list.

---

## 11. Manager Chat Scope & UI Location

### Scope Definition
Staff/Managers participate in **`TECHNICIAN_MANAGER`** conversations.
- This is a dedicated 1-on-1 dialogue between the **assigned Technician** and **Staff/Manager** regarding a specific repair request.
- It is **not** a 3-party group chat with the Customer.

### Recommended UI Placement
- **Request Detail Page ([`requests/[id].vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/pages/requests/[id].vue))**: Under a dedicated "Technician Chat" tab or side panel alongside the Technician Assignment section.
- **Top Navigation Bar ([`NavMessages.vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/components/NavMessages.vue))**: Quick dropdown showing unread manager conversations and linking directly to the request chat.

---

## 12. Staff Chat REST API

Base Path: `/api/v1/conversations` (proxied in Nuxt via `/api/conversations`)

| Endpoint | Method | Role & Description |
| :--- | :--- | :--- |
| `/conversations` | `GET` | Paginated summary list of active conversations for staff (`Page<ConversationSummaryResponse>`). |
| `/conversations/{id}` | `GET` | Detailed conversation information with participant list and computed `unreadCount`. |
| `/conversations/{id}/messages` | `GET` | Message history with backward cursor pagination (`?beforeId=<id>&size=20`). |
| `/conversations/{id}/messages` | `POST` | Send a new message (idempotent via `clientMessageId`). |
| `/conversations/{id}/read` | `POST` | Mark conversation read up to `messageId`. |
| `/conversations/requests/{requestId}/technician-manager` | `POST` | Look up or auto-create the `TECHNICIAN_MANAGER` conversation for a repair request. |
| `/conversations/requests/{requestId}/customer-technician` | `POST` | Look up the `CUSTOMER_TECHNICIAN` conversation for operational review. *(Read access; sending requires active participant status).* |

---

## 13. Chat STOMP Actions

Clients can perform realtime chat actions via `/app` destinations:

### 1. Send Message: `/app/chat.send`
```json
{
  "conversationId": 12,
  "clientMessageId": "7b8f9e6a-2d4e-4f7a-9c8b-1a2b3c4d5e6f",
  "type": "TEXT",
  "text": "Please provide an updated parts estimate.",
  "attachmentId": null,
  "replyToMessageId": null
}
```

### 2. Mark Read: `/app/chat.read`
```json
{
  "conversationId": 12,
  "messageId": 1450
}
```

### 3. Typing Indicator: `/app/chat.typing`
```json
{
  "conversationId": 12,
  "typing": true
}
```

---

## 14. Chat Message Types & Idempotency

### Supported Message Types
- `TEXT`: Requires non-blank `text` (max 4000 characters).
- `IMAGE`: Requires `attachmentId` of an available photo.
- `FILE`: Requires `attachmentId` of an available document.

*(Note: `DOCUMENT`, `LOCATION`, and `SYSTEM` are not chat message types).*

### Client Message Idempotency
- Every outgoing message requires a unique `clientMessageId` (UUID string, max 64 chars).
- If network disconnection causes a retry, the frontend **must keep the same `clientMessageId`**. The backend will return the already persisted message without duplicate events or side effects.

---

## 15. Attachment Upload Integration

Each chat message supports **exactly one `attachmentId`**.

### Upload Flow for Chat Attachments
```text
User selects image/file in Chat UI
                 ↓
Upload file via existing REST endpoint:
POST /api/v1/requests/{requestId}/attachments
(multipart/form-data: type='GENERAL_DOCUMENT' or 'DIAGNOSIS_PHOTO', file=<File>)
                 ↓
Backend returns Attachment entity with id (status: 'AVAILABLE')
                 ↓
Send chat message:
POST /api/v1/conversations/{id}/messages OR STOMP /app/chat.send
{
  "conversationId": 12,
  "clientMessageId": "...",
  "type": "IMAGE" (or "FILE"),
  "attachmentId": attachment.id
}
```
*Do not send binary data or base64 over WebSocket.*

---

## 16. Read State & Unread Counts

- The database stores `conversation_participants.last_read_message_id`.
- `unreadCount` is dynamically computed by the backend on conversation summaries.
- When the manager opens a conversation or receives messages while active:
  1. Call `/app/chat.read` or `POST /api/v1/conversations/{id}/read`.
  2. The server advances `last_read_message_id` and broadcasts `CHAT_MESSAGE_READ` to participants.
  3. The frontend updates local unread badges without maintaining conflicting permanent counters.

---

## 17. Typing Indicators

- Received over `/user/queue/chat` as `CHAT_TYPING_STARTED` and `CHAT_TYPING_STOPPED`.
- Payload: `{ "conversationId": 12, "actorType": "TECHNICIAN", "actorId": 18, "typing": true }`.
- **Frontend Behavior**: Displays ephemeral text (e.g. *"Technician is typing..."*) and automatically clears after 3 seconds or on `CHAT_TYPING_STOPPED` / `CHAT_MESSAGE_CREATED`.
- **Sending**: Throttle typing notifications on the frontend (e.g. 1 per 2 seconds).

---

## 18. Technician Reassignment & Lifecycle Rules

The `CUSTOMER_TECHNICIAN` and `TECHNICIAN_MANAGER` conversations belong to the **repair request**:

1. **Removed Technician**:
   - Participant status is set to `leftAt`.
   - Stops receiving future realtime events.
   - Message history is truncated at `leftAt` (cannot read subsequent messages).
2. **Newly Assigned Technician**:
   - Added as an active participant.
   - **Receives access to full request conversation history** (including prior messages) to preserve operational context.
3. **Terminal Request State**:
   - When request is `COMPLETED` or `CANCELLED`, conversations become **read-only**.
   - Attempting to send messages returns HTTP `409 Conflict` with `CONVERSATION_READ_ONLY`.
   - The frontend should disable message input controls when request status is terminal.

---

## 19. Reconnect & REST Catch-Up Strategy

When the WebSocket reconnects after a network drop or server restart:
1. Re-establish STOMP subscriptions (`/topic/staff.events`, `/user/queue/events`, `/user/queue/chat`).
2. **Execute REST catch-up** (WebSockets must not be assumed to queue missed events offline):
   - Refetch open request list (`refreshNuxtData('requests-list')`).
   - Refetch active request detail if open (`refreshRequestData()`).
   - Refetch dashboard (`refreshNuxtData('dashboard')`).
   - Refetch active conversation messages (`/messages?beforeId=...`).

---

## 20. Logout & Session Cleanup

In [`useAuth.ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/composables/useAuth.ts):
```typescript
async function logout() {
  // 1. Immediately disconnect realtime STOMP client
  realtimeService.disconnect()
  
  // 2. Clear tokens and auth user state
  // 3. Navigate to /login
}
```
*No WebSocket connection may remain active after user logout.*

---

## 21. Localization Responsibilities

- Backend domain events emit enum codes (`NEW`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `COMPLETED`, `CANCELLED`).
- The frontend translates all labels and status badges using [`useLocale()`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/composables/useLocale.ts) and translation files (`app/locales/{uz,ru,en}.ts`).
- Chat message text entered by users is displayed verbatim without transformation.

---

## 22. Error Handling

Handle standard backend error structures via [`getApiErrorMessage`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/utils/api.ts):

| Backend Error Code | HTTP Status | Frontend Action |
| :--- | :--- | :--- |
| `AUTHENTICATION_REQUIRED` | 401 | Trigger `refreshSession()` or redirect to `/login`. |
| `ACCESS_DENIED` | 403 | Show permission error notification. |
| `CONVERSATION_READ_ONLY` | 409 | Disable chat input and notify staff that request is completed/cancelled. |
| `ATTACHMENT_NOT_AVAILABLE` | 404 | Prompt staff to re-upload attachment. |
| `ATTACHMENT_FORBIDDEN` | 403 | Prevent sending attachment belonging to another request. |
| `CLIENT_MESSAGE_ID_REQUIRED`| 400 | Ensure UUID generator generates a valid client message ID. |

---

## 23. Query / Event Invalidation Matrix

| Realtime Event | Channel | Frontend Action |
| :--- | :--- | :--- |
| `REQUEST_CREATED` | `/topic/staff.events` | `refreshNuxtData('requests-list')`, `refreshNuxtData('dashboard-recent')` |
| `REQUEST_UPDATED` | `/topic/staff.events` | `refreshNuxtData('requests-list')`, `refreshNuxtData('request-' + id)` |
| `REQUEST_ASSIGNED` | `/topic/staff.events` | `refreshNuxtData('requests-list')`, `refreshNuxtData('request-' + id)` |
| `REQUEST_UNASSIGNED` | `/topic/staff.events` | `refreshNuxtData('requests-list')`, `refreshNuxtData('request-' + id)` |
| `REQUEST_STATUS_CHANGED`| `/topic/staff.events` | `refreshNuxtData('requests-list')`, `refreshNuxtData('request-' + id)`, `refreshNuxtData('dashboard')` |
| `DASHBOARD_INVALIDATED` | `/topic/staff.events` | Debounced (400ms) `refreshNuxtData(['dashboard', 'dashboard-recent'])` |
| `NOTIFICATION_CREATED` | `/user/queue/events` | Refetch topbar notification list / badge |
| `CHAT_MESSAGE_CREATED` | `/user/queue/chat` | Append message to active chat, advance read state, or increment badge in `NavMessages` |
| `CHAT_MESSAGE_READ` | `/user/queue/chat` | Update read checkmarks on sent messages |
| `CHAT_TYPING_STARTED` | `/user/queue/chat` | Show "Technician is typing..." |
| `CHAT_TYPING_STOPPED` | `/user/queue/chat` | Hide typing indicator |

---

## 24. Expected Frontend Files / Modules to Change

| File Path | Nature of Eventual Change |
| :--- | :--- |
| [`repairSystem/app/composables/useAuth.ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/composables/useAuth.ts) | Hook `realtimeService.connect()` on login/init, `realtimeService.reconnect(newToken)` on `refreshSession()`, and `realtimeService.disconnect()` on `logout()`. |
| [`repairSystem/app/layouts/default.vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/layouts/default.vue) | Initialize the realtime connection listener when the user is authenticated in the default layout. |
| [`repairSystem/app/pages/requests/index.vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/pages/requests/index.vue) | Register event listener for `REQUEST_*` events to refresh request list query. |
| [`repairSystem/app/pages/requests/[id].vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/pages/requests/[id].vue) | Register listener for request-specific events (`requestId === id`) and embed the Manager Chat component. |
| [`repairSystem/app/pages/index.vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/pages/index.vue) | Register listener for `DASHBOARD_INVALIDATED` with debounced refresh. |
| [`repairSystem/app/components/AppTopbar.vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/components/AppTopbar.vue) | Connect `NavNotifications` and `NavMessages` to realtime update feeds. |
| [`repairSystem/app/components/NavMessages.vue`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/components/NavMessages.vue) | Bind to active manager conversations and unread count state. |
| [`repairSystem/app/types/index.ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/types/index.ts) | Add Chat DTO types (`ChatMessage`, `ConversationSummary`, `RealtimeEvent`, etc.). |
| [`repairSystem/app/locales/{en,ru,uz}.ts`](file:///c:/Users/User/Documents/RepairSystem/repairSystem/app/locales/en.ts) | Add localized strings for chat actions, typing states, and connection status. |

---

## 25. New Modules Likely Needed

1. `app/composables/useRealtime.ts`: Singleton STOMP client composable managing connection, subscriptions, reconnect logic, and event bus distribution.
2. `app/composables/useManagerChat.ts`: Chat state composable for loading history, sending messages, handling typing indicators, and tracking read status.
3. `app/components/chat/ManagerChatBox.vue`: Reusable chat window component embedded on the Request Detail page.
4. `app/types/chat.ts`: TypeScript interfaces for chat payloads, STOMP messages, and conversation summaries.

---

## 26. Dependency Audit

- **Current `package.json`**: Contains `admin-lte`, `bootstrap`, `nuxt`, `apexcharts`.
- **STOMP Client**: Currently **not installed**.
- **Recommendation for Implementation**: Install `@stomp/stompjs` (standard native browser WebSocket STOMP client without Node dependencies).
  ```bash
  pnpm add @stomp/stompjs
  ```

---

## 27. Security Requirements Summary

1. **No Tokens in URLs**: Always pass access tokens in the STOMP `CONNECT` header.
2. **Session Cleanup**: Always disconnect WebSockets on user logout or session expiration.
3. **Reconnect on Token Refresh**: Disconnect and reconnect STOMP whenever REST issues a new access token.
4. **Destination Isolation**: Never attempt subscribing to unauthorized paths (e.g. `/queue/...` or customer queues).
5. **Authoritative REST**: Always rely on REST API queries for persistent state; use WebSockets exclusively as invalidation and push notifications.

---

## 28. Frontend Integration Acceptance Criteria

1. **Automatic Request List Invalidation**: When a new repair request is created in the backend, the Admin/Manager requests table updates immediately without manual page refresh.
2. **Automatic Request Detail Sync**: When a technician accepts an assignment or updates execution diagnosis, an open Request Detail view refreshes its data automatically.
3. **Live Dashboard Statistics**: Dashboard KPIs and charts update on `DASHBOARD_INVALIDATED` with debounced refetches.
4. **Technician ↔ Manager Realtime Chat**:
   - Manager can send text and image attachments to the assigned technician.
   - Incoming messages from technician appear instantly via `/user/queue/chat`.
   - Read checkmarks advance when the other participant reads the message.
   - Ephemeral typing indicators display when the other party types.
5. **Token Refresh Resilience**: When the JWT access token is refreshed, the STOMP connection transparently reconnects with the new token and resumes listening without message loss.
6. **Graceful Network Reconnect**: If network connectivity drops and recovers, the client reconnects, resubscribes, and performs REST catch-up.
7. **Clean Logout**: On clicking "Sign Out", the WebSocket connection is immediately terminated.
