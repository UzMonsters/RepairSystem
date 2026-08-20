# Realtime WebSocket and Chat Integration Contract

This is the client contract for the current RepairAuto backend implementation. The REST API and PostgreSQL state are authoritative. WebSocket/STOMP events are used for invalidation, catch-up prompts, private notifications, and chat fan-out after durable state has been committed.

## Architecture

- Native STOMP WebSocket endpoint: `/ws`
- SockJS fallback: also configured on `/ws`; mobile clients may use native WebSocket/STOMP and do not need SockJS.
- Application destination prefix: `/app`
- User destination prefix: `/user`
- Broker prefixes: `/queue`, `/topic`
- Durable events are published from Spring application events with `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- Typing events are ephemeral, are not stored in the database, and are sent immediately after participant authorization and throttling.

## Authentication

Authenticate the STOMP `CONNECT` frame with the same JWT access token used by REST:

```text
CONNECT
Authorization:Bearer <JWT_ACCESS_TOKEN>
```

`authorization` is also accepted case-insensitively. The backend strips the optional `Bearer ` prefix, validates through `JwtTokenService`, and attaches the authenticated principal to the WebSocket session.

Supported principals:

- Staff: `ActorType.STAFF`, `ROLE_ADMIN` or `ROLE_MANAGER`
- Customer mobile actor: `ActorType.CUSTOMER`, `ROLE_CUSTOMER`
- Technician mobile actor: `ActorType.TECHNICIAN`, `ROLE_TECHNICIAN`

Do not put tokens in WebSocket URLs.

### WebSocket Connection & JWT Lifetime
- Authentication is validated upon the initial STOMP `CONNECT` frame.
- If a client's JWT access token expires while the WebSocket connection remains physically open, the established STOMP session remains authenticated until disconnection.
- Clients must actively disconnect and reconnect with the newly issued token whenever they refresh their access token or re-authenticate via REST.

## Subscriptions

| Destination | Permitted actors | Contents |
| --- | --- | --- |
| `/user/queue/events` | authenticated staff, customer, technician | User-scoped non-chat domain events (`REQUEST_*`, `NOTIFICATION_*`, `DASHBOARD_INVALIDATED`). |
| `/user/queue/chat` | authenticated staff, customer, technician | User-scoped `CHAT_*` events exclusively (`CHAT_MESSAGE_CREATED`, `CHAT_MESSAGE_READ`, `CHAT_TYPING_STARTED`, `CHAT_TYPING_STOPPED`). |
| `/topic/staff.events` | staff principals only; staff roles are `ADMIN` and `MANAGER` | Staff-wide request lifecycle and dashboard invalidation events. Chat messages and private notifications are not published to this topic. |

Clients may not subscribe directly to `/queue/...`, and arbitrary `/topic/...` subscriptions are rejected except the staff topic above.

## Event Envelope

All realtime events serialize as:

```json
{
  "eventId": "c70cb4fb-2a66-4d33-93b0-84ddfed9eaf6",
  "type": "CHAT_MESSAGE_CREATED",
  "occurredAt": "2026-08-20T07:15:30Z",
  "payload": {}
}
```

`eventId` is generated per publish. `occurredAt` is an `Instant`.

## Event Catalog

Only the following event types exist in `RealtimeEventType`.

### `REQUEST_CREATED`

Sent to `/topic/staff.events`, active staff user queues, and the customer's `/user/queue/events`.

```json
{
  "requestId": 105,
  "requestNumber": "REP-2026-000105",
  "customerId": 42,
  "technicianId": null,
  "status": "NEW",
  "priority": null
}
```

### `REQUEST_UPDATED`

Sent to `/topic/staff.events`, active staff user queues, and the customer's `/user/queue/events`.

```json
{
  "requestId": 105,
  "requestNumber": "REP-2026-000105",
  "customerId": 42,
  "technicianId": null,
  "status": null,
  "priority": null
}
```

### `REQUEST_ASSIGNED`

Sent to `/topic/staff.events`, active staff user queues, customer `/user/queue/events`, and technician `/user/queue/events`.

```json
{
  "requestId": 105,
  "requestNumber": "REP-2026-000105",
  "customerId": 42,
  "technicianId": 18,
  "status": "ASSIGNED",
  "priority": null
}
```

### `REQUEST_UNASSIGNED`

Sent to `/topic/staff.events`, active staff user queues, customer `/user/queue/events`, and the removed technician's `/user/queue/events`.

```json
{
  "requestId": 105,
  "requestNumber": "REP-2026-000105",
  "customerId": 42,
  "technicianId": 18,
  "status": "NEW",
  "priority": null
}
```

### `REQUEST_STATUS_CHANGED`

Sent to `/topic/staff.events`, active staff user queues, customer `/user/queue/events`, and technician `/user/queue/events` when a technician is known.

```json
{
  "requestId": 105,
  "requestNumber": "REP-2026-000105",
  "customerId": 42,
  "technicianId": 18,
  "status": "IN_PROGRESS",
  "priority": null
}
```

### `DASHBOARD_INVALIDATED`

Sent to `/topic/staff.events` and active staff user queues for request creation, assignment, unassignment, status changes, and explicit dashboard invalidation events.

```json
{
  "reason": "REQUEST_STATUS_CHANGED"
}
```

### `NOTIFICATION_CREATED`

Sent only to the recipient's `/user/queue/events`. Staff notification inbox records are currently skipped by `UserNotificationService`; this event is created for customer and technician inbox notifications.

```json
{
  "notificationId": 320,
  "notificationType": "TECHNICIAN_ASSIGNED",
  "targetId": 105,
  "target": "REPAIR_REQUEST_DETAILS",
  "read": false
}
```

### `NOTIFICATION_READ`

Sent only to the recipient's `/user/queue/events`.

```json
{
  "notificationId": 320,
  "notificationType": null,
  "targetId": null,
  "target": null,
  "read": true
}
```

For "mark all as read", `notificationId` is `null`.

### `CHAT_MESSAGE_CREATED`

Sent to the sender and active conversation participants exclusively on `/user/queue/chat`. Non-chat events are delivered separately via `/user/queue/events`.

```json
{
  "messageId": 1450,
  "conversationId": 12,
  "senderType": "CUSTOMER",
  "senderId": 42,
  "clientMessageId": "d8e3b4a2-71c9-4b68-80f4-5f1234567890",
  "messageType": "TEXT",
  "text": "Hello, I will be home at 3 PM.",
  "attachmentId": null,
  "replyToMessageId": null,
  "createdAt": "2026-08-20T07:15:30Z"
}
```

### `CHAT_MESSAGE_READ`

Sent to other active conversation participants on user destinations.

```json
{
  "conversationId": 12,
  "messageId": 1450,
  "readerType": "TECHNICIAN",
  "readerId": 18,
  "readAt": "2026-08-20T07:15:35Z"
}
```

### `CHAT_TYPING_STARTED` and `CHAT_TYPING_STOPPED`

Sent to other active conversation participants on user destinations. Not persisted.

```json
{
  "conversationId": 12,
  "actorType": "CUSTOMER",
  "actorId": 42,
  "typing": true
}
```

Typing is throttled to one event per actor per conversation every 2 seconds.

## Client Send Destinations

### `/app/chat.send`

```json
{
  "conversationId": 12,
  "clientMessageId": "d8e3b4a2-71c9-4b68-80f4-5f1234567890",
  "type": "TEXT",
  "text": "Hello, I will be home at 3 PM.",
  "attachmentId": null,
  "replyToMessageId": null
}
```

### `/app/chat.read`

```json
{
  "conversationId": 12,
  "messageId": 1450
}
```

### `/app/chat.typing`

```json
{
  "conversationId": 12,
  "typing": true
}
```

## Mobile REST API

Base path: `/api/v1/mobile/me/conversations`

Requires `ROLE_CUSTOMER` or `ROLE_TECHNICIAN`.

- `GET /api/v1/mobile/me/conversations?page=0&size=20`
- `GET /api/v1/mobile/me/conversations/{id}`
- `GET /api/v1/mobile/me/conversations/{id}/messages?beforeId=1450&page=0&size=20`
- `POST /api/v1/mobile/me/conversations/{id}/messages`
- `POST /api/v1/mobile/me/conversations/{id}/read`
- `POST /api/v1/mobile/me/conversations/requests/{requestId}`

The request lookup/create endpoint creates or returns the `CUSTOMER_TECHNICIAN` conversation only after the mobile actor is authorized for that repair request. Customers must own the request. Technicians must be the current active technician assignment.

## Staff REST API

Base path: `/api/v1/conversations`

Requires `ROLE_ADMIN` or `ROLE_MANAGER`.

- `GET /api/v1/conversations?page=0&size=20`
- `GET /api/v1/conversations/{id}`
- `GET /api/v1/conversations/{id}/messages?beforeId=1450&page=0&size=20`
- `POST /api/v1/conversations/{id}/messages`
- `POST /api/v1/conversations/{id}/read`
- `POST /api/v1/conversations/requests/{requestId}/technician-manager`
- `POST /api/v1/conversations/requests/{requestId}/customer-technician`

Staff can create/join the `TECHNICIAN_MANAGER` conversation for a request. Staff can look up the `CUSTOMER_TECHNICIAN` request conversation, but sending still requires an active participant row.

## REST Chat DTOs

`SendMessageRequest`:

```json
{
  "conversationId": 12,
  "clientMessageId": "client-uuid-or-stable-id",
  "type": "TEXT",
  "text": "Message body",
  "attachmentId": null,
  "replyToMessageId": null
}
```

`ChatMessageResponse`:

```json
{
  "id": 1450,
  "conversationId": 12,
  "senderType": "CUSTOMER",
  "senderId": 42,
  "clientMessageId": "client-uuid-or-stable-id",
  "messageType": "TEXT",
  "text": "Message body",
  "attachmentId": null,
  "replyToMessageId": null,
  "createdAt": "2026-08-20T12:15:30Z",
  "editedAt": null,
  "deletedAt": null
}
```

Conversation summaries/details include `id`, `repairRequestId`, `requestNumber`, `conversationType`, `status`, `unreadCount`, `participants`, timestamps, and `lastMessage` for summaries.

## Chat Types

Conversation types:

- `CUSTOMER_TECHNICIAN`: a repair-request-scoped customer and active technician conversation.
- `TECHNICIAN_MANAGER`: a separate repair-request-scoped technician and staff/manager conversation. It is not a customer + technician + manager group chat.

Conversation statuses:

- `ACTIVE`
- `CLOSED`

Message types:

- `TEXT`: requires nonblank `text`, max 4000 characters.
- `IMAGE`: requires `attachmentId`.
- `FILE`: requires `attachmentId`.

There is no `DOCUMENT`, `LOCATION`, or `SYSTEM` chat message type in the current backend contract.

## Attachments

Chat messages support exactly one attachment through `attachmentId`.

For `IMAGE` and `FILE` messages:

- `attachmentId` is required.
- The attachment must exist.
- The attachment status must be `AVAILABLE`.
- The attachment must belong to the same repair request as the conversation.
- The sender must be an active participant in that conversation.

Selecting multiple images/files on mobile requires sending multiple chat messages, each with its own `clientMessageId`.

## Idempotency

`clientMessageId` is required and capped at 64 characters. The database uniqueness key is:

```text
conversation_id + sender_type + sender_id + client_message_id
```

If the same sender retries the same `clientMessageId` in the same conversation, the server returns the existing persisted message. It does not create another row, publish another realtime message event, or send another chat push.

## Read State

There is no persisted `unread_count` column. `conversation_participants.last_read_message_id` is the authoritative read pointer. `unreadCount` is computed for conversation responses from message IDs greater than the participant's last read message.

When a sender creates a message, their own `lastReadMessageId` is advanced to that message. `POST .../read` and `/app/chat.read` advance the pointer only forward.

## Lifecycle and Technician Reassignment

The `CUSTOMER_TECHNICIAN` conversation belongs to the repair request.

### Technician Reassignment Rules
- **Removed Technician (Technician A)**:
  - When a technician is unassigned or replaced, their participant row is marked with `leftAt`.
  - They immediately stop receiving future realtime chat events (`/user/queue/chat`).
  - They cannot send new messages (`ACCESS_DENIED`).
  - REST history for the removed technician is restricted to messages created at or before `leftAt`; messages created after `leftAt` are not accessible.
- **Newly Assigned Technician (Technician B)**:
  - When a new technician is assigned, they are added as an active participant to the existing request conversation (or reactivated if they previously participated).
  - The new technician receives full access to the existing request conversation history (including messages exchanged prior to their assignment) to ensure operational continuity.
  - The new technician receives all future realtime chat events normally while remaining the active participant.
- **Terminal Request State**:
  - When the repair request reaches `COMPLETED` or `CANCELLED`, REST and STOMP chat sends are rejected with `CONVERSATION_READ_ONLY` (HTTP 409).
  - Conversation history remains readable subject to participant access rules.

## Reconnect and Catch-Up

Clients should treat realtime events as invalidation and prompt signals. On reconnect, fetch authoritative state through REST:

- Conversations: `GET /api/v1/mobile/me/conversations` or `GET /api/v1/conversations`
- Message history: `GET .../conversations/{id}/messages?beforeId=...`
- Request lists/details: normal repair request REST endpoints
- Notifications: mobile notification inbox endpoints
- Dashboard: normal dashboard REST endpoint

The backend does not send complete paginated request lists or dashboard models over WebSocket.

## FCM Chat Push

Chat push uses the existing push endpoint infrastructure:

- No new push registration system is introduced.
- Recipient endpoints are loaded from existing enabled push endpoints for staff, customers, or technicians.
- The sender does not receive their own push.
- Payload data includes `type=CHAT_MESSAGE`, `conversationId`, `messageId`, `repairRequestId` when present, `requestNumber` when present, `senderType`, and `senderId`.
- Push is triggered after the chat message transaction commits.
- Duplicate `clientMessageId` retries do not trigger duplicate pushes.

The current backend does not track per-conversation foreground viewing/subscription state for push suppression. It sends chat push to all enabled recipient endpoints except the sender whenever Firebase push is enabled.

## Frontend Query Invalidation

Recommended client reactions:

- `REQUEST_CREATED`, `REQUEST_UPDATED`, `REQUEST_ASSIGNED`, `REQUEST_UNASSIGNED`, `REQUEST_STATUS_CHANGED`: invalidate repair request lists and affected request details.
- `DASHBOARD_INVALIDATED`: debounce and refetch dashboard data.
- `NOTIFICATION_CREATED`, `NOTIFICATION_READ`: invalidate notification lists and unread counts.
- `CHAT_MESSAGE_CREATED`, `CHAT_MESSAGE_READ`: invalidate conversation list, affected conversation details, and affected message history.
- `CHAT_TYPING_STARTED`, `CHAT_TYPING_STOPPED`: update only ephemeral typing UI.
