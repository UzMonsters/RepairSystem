# RepairAuto Realtime — Staff Web Frontend Implementation Plan

## 1. Executive Summary & Context

This implementation plan provides the definitive specifications, destination contracts, verified payload schemas, and concrete guidance for the **Staff Web Frontend (`repairSystem/`)** developer.

The backend WebSocket and STOMP architecture has been upgraded to a deterministic, private user queue delivery system. All duplicate broadcasting destinations (such as `/topic/staff.events`) have been removed from the backend in favor of `/user/queue/events` and `/user/queue/chat`.

---

## 2. Transport & Browser Authentication Contract

### 2.1 WebSocket Endpoint & Protocol
- **Endpoint URL**: `wss://repair-auto.onrender.com/ws` (or local `ws://localhost:8080/ws`)
- **Protocol**: STOMP over WebSocket (e.g. `@stomp/stompjs` client)

### 2.2 Browser vs Mobile Authentication Contract
- **Staff Web Browser Client**:
  - The standard browser `WebSocket` JavaScript API does **not** permit arbitrary HTTP request headers during the initial WebSocket handshake.
  - Staff web clients **must** authenticate strictly via the STOMP `CONNECT` frame header:
    ```javascript
    const client = new Client({
      brokerURL: 'wss://repair-auto.onrender.com/ws',
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      // ...
    })
    ```
- **Mobile / Native Clients**:
  - Native clients can supply headers during both the HTTP handshake and the STOMP `CONNECT` frame, but web browser clients rely exclusively on the STOMP `CONNECT` frame header.

### 2.3 Reconnection & Token Refresh Policy
- When the JWT access token is refreshed or renewed:
  1. Store the new access token in the `access_token` cookie.
  2. Deactivate the active STOMP client: `await client.deactivate()`.
  3. Re-activate the STOMP client with the updated `Authorization: Bearer <NEW_TOKEN>` header.
- Upon reconnection (`onConnect`):
  1. Subscribe to `/user/queue/events` (for request lifecycle, assignment, and dashboard invalidation events).
  2. Subscribe to `/user/queue/chat` (for chat messages, read receipts, and typing indicators).
  3. Trigger Nuxt data reconciliation (`refreshNuxtData(...)`) to re-sync any state missed during the disconnected interval.

---

## 3. STOMP Subscription Destinations

| Channel Destination | Event Scope | Action Required for Web Staff |
| :--- | :--- | :--- |
| `/user/queue/events` | Domain events (`REQUEST_*`, `DASHBOARD_INVALIDATED`) | **Subscribe** |
| `/user/queue/chat` | Chat events (`CHAT_MESSAGE_CREATED`, `CHAT_MESSAGE_READ`, `CHAT_TYPING_*`) | **Subscribe** |
| `/topic/staff.events` | **DEPRECATED & REMOVED** | **Remove subscription** (Do not subscribe; backend no longer broadcasts to topic) |

---

## 4. Realtime Event Envelope & JSON Schemas

### 4.1 Root Realtime Envelope Schema
Every frame delivered to `/user/queue/events` and `/user/queue/chat` matches `RealtimeEvent.java`:

```typescript
export interface RealtimeEnvelope<T = Record<string, any>> {
  eventId: string // UUID string, e.g. "d3b07384-d113-494a-8141-382a933fef57"
  type: RealtimeEventType
  occurredAt: string // ISO 8601 UTC Instant, e.g. "2026-08-29T10:00:00Z"
  payload: T
}
```

### 4.2 Complete Event Types (`RealtimeEventType` — 20 Types)
The backend enum `com.example.darks.repair_auto.realtime.event.RealtimeEventType` defines **20** event types:

```typescript
export type RealtimeEventType =
  // Request Lifecycle & Assignment (13)
  | 'REQUEST_CREATED'
  | 'REQUEST_UPDATED'
  | 'REQUEST_ASSIGNED'
  | 'REQUEST_ASSIGNMENT_CREATED'
  | 'REQUEST_ASSIGNMENT_ACCEPTED'
  | 'REQUEST_ASSIGNMENT_REJECTED'
  | 'REQUEST_REASSIGNED'
  | 'REQUEST_UNASSIGNED'
  | 'REQUEST_SCHEDULE_CHANGED'
  | 'REQUEST_DIAGNOSIS_UPDATED'
  | 'REQUEST_ATTACHMENTS_CHANGED'
  | 'REQUEST_STATUS_CHANGED'
  | 'REQUEST_DELETED'
  // Analytics & Dashboard (1)
  | 'DASHBOARD_INVALIDATED'
  // Mobile User Notifications (2 - Mobile inboxes only, see Section 6)
  | 'NOTIFICATION_CREATED'
  | 'NOTIFICATION_READ'
  // Realtime Chat (4)
  | 'CHAT_MESSAGE_CREATED'
  | 'CHAT_MESSAGE_READ'
  | 'CHAT_TYPING_STARTED'
  | 'CHAT_TYPING_STOPPED'
```

---

## 5. Exact Backend Payload Schemas

### 5.1 Request Event Payload (`REQUEST_CREATED`, `REQUEST_UPDATED`, `REQUEST_STATUS_CHANGED`)
*Backend Source: `RequestEventPayload.java`*
```typescript
export interface RequestEventPayload {
  requestId: number
  requestNumber: string | null
  customerId: number | null
  technicianId: number | null
  status: string | null // 'NEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'SCHEDULED' | 'WAITING_FOR_PARTS' | 'COMPLETED' | 'CANCELLED'
  oldStatus: string | null
  priority: string | null // 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
}
```

### 5.2 Assignment Event Payload (`REQUEST_ASSIGNED`, `REQUEST_ASSIGNMENT_CREATED`, `REQUEST_ASSIGNMENT_ACCEPTED`, `REQUEST_ASSIGNMENT_REJECTED`, `REQUEST_REASSIGNED`, `REQUEST_UNASSIGNED`)
*Backend Source: `AssignmentEventPayload.java`*
```typescript
export interface AssignmentEventPayload {
  requestId: number
  requestNumber: string | null
  assignmentId: number | null
  technicianId: number | null
  previousTechnicianId: number | null
  customerId: number | null
  action: string | null // 'CREATED' | 'ACCEPTED' | 'REJECTED' | 'REASSIGNED' | 'UNASSIGNED' | 'ASSIGNED'
  status: string | null // 'ASSIGNED' | 'REJECTED' | 'NEW'
}
```

### 5.3 Schedule Event Payload (`REQUEST_SCHEDULE_CHANGED`)
*Backend Source: `ScheduleEventPayload.java`*
```typescript
export interface ScheduleEventPayload {
  requestId: number
  requestNumber: string | null
  assignmentId: number | null
  technicianId: number | null
  customerId: number | null
  scheduledStart: string | null // ISO 8601 Instant
  scheduledEnd: string | null // ISO 8601 Instant
  scheduleAction: string | null // 'SCHEDULED' | 'RESCHEDULED' | 'CANCELLED'
}
```

### 5.4 Diagnosis Event Payload (`REQUEST_DIAGNOSIS_UPDATED`)
*Backend Source: `DiagnosisEventPayload.java`*
```typescript
export interface DiagnosisEventPayload {
  requestId: number
  requestNumber: string | null
  executionId: number | null
  technicianId: number | null
  customerId: number | null
}
```

### 5.5 Attachment Event Payload (`REQUEST_ATTACHMENTS_CHANGED`)
*Backend Source: `AttachmentEventPayload.java`*
```typescript
export interface AttachmentEventPayload {
  requestId: number
  requestNumber: string | null
  attachmentId: number | null
  changeType: string | null // 'UPLOADED' | 'DELETED'
  customerId: number | null
  technicianId: number | null
}
```

### 5.6 Request Deleted Payload (`REQUEST_DELETED`)
*Backend Source: `RequestDeletedPayload.java`*
```typescript
export interface RequestDeletedPayload {
  requestId: number
  requestNumber: string | null
  customerId: number | null
  technicianId: number | null
}
```

### 5.7 Dashboard Invalidation Payload (`DASHBOARD_INVALIDATED`)
*Backend Source: `DashboardInvalidatedPayload.java`*
```typescript
export interface DashboardInvalidatedPayload {
  reason: string | null
}
```

### 5.8 Chat Message Payload (`CHAT_MESSAGE_CREATED`)
*Backend Source: `ChatMessagePayload.java`*
> [!IMPORTANT]
> **Field Name Warning**: In the backend realtime event payload, the primary key field is named `messageId` (numeric ID). When mapping to local Vue components expecting `id`, map `id = payload.messageId`.

```typescript
export interface ChatMessagePayload {
  messageId: number
  conversationId: number
  senderType: 'CUSTOMER' | 'TECHNICIAN' | 'STAFF'
  senderId: number
  clientMessageId: string | null
  messageType: 'TEXT' | 'IMAGE' | 'FILE'
  text: string | null
  attachmentId: number | null
  replyToMessageId: number | null
  createdAt: string | null // ISO 8601 Instant
}
```

### 5.9 Chat Read Receipt Payload (`CHAT_MESSAGE_READ`)
*Backend Source: `ChatReadPayload.java`*
```typescript
export interface ChatReadPayload {
  conversationId: number
  messageId: number
  readerType: 'CUSTOMER' | 'TECHNICIAN' | 'STAFF'
  readerId: number
  readAt: string | null // ISO 8601 Instant
}
```

### 5.10 Chat Typing Payload (`CHAT_TYPING_STARTED`, `CHAT_TYPING_STOPPED`)
*Backend Source: `ChatTypingPayload.java`*
```typescript
export interface ChatTypingPayload {
  conversationId: number
  actorType: 'CUSTOMER' | 'TECHNICIAN' | 'STAFF'
  actorId: number
  typing: boolean
}
```

---

## 6. Staff Notifications: Realtime Verification Finding

### Code Verification
Inspection of `UserNotificationService.java` (line 90) shows:
```java
if (event.recipientType() == NotificationRecipientType.STAFF) {
    return RecordResult.SKIPPED;
}
```
- **Finding**: In the backend architecture, `UserNotification` records and realtime notification events (`NOTIFICATION_CREATED`, `NOTIFICATION_READ`) are implemented exclusively for mobile customer and technician inboxes (`/api/v1/mobile/me/notifications`).
- **Staff Notification Architecture**: Staff users do **not** have an inbox entity and do **not** receive `NOTIFICATION_CREATED` / `NOTIFICATION_READ` events over WebSocket.
- **Guidance for `NavNotifications.vue`**:
  - Do **not** subscribe `NavNotifications.vue` to `NOTIFICATION_CREATED` or `NOTIFICATION_READ`.
  - Staff notifications/alerts on the web continue to rely on standard REST endpoints and dashboard invalidation signals (`DASHBOARD_INVALIDATED` / `REQUEST_*`).

---

## 7. Staff Web Frontend File Changes Required

### 7.1 `repairSystem/app/types/realtime.ts`
1. Update `RealtimeEventType` to match all 20 event types.
2. Update `RealtimeEvent` interface to use `occurredAt: string` and `eventId: string`.
3. Add the exact payload interfaces from Section 5.

### 7.2 `repairSystem/app/composables/useRealtime.ts`
1. **Remove deprecated subscription**:
   ```typescript
   // REMOVE THIS LINE:
   // client?.subscribe('/topic/staff.events', handleMessage)
   ```
2. **Maintain user queue subscriptions**:
   ```typescript
   client?.subscribe('/user/queue/events', handleMessage)
   client?.subscribe('/user/queue/chat', handleMessage)
   ```
3. **Broaden request event triggers**:
   Include all request domain events in `requestEvents`:
   ```typescript
   const requestEvents: RealtimeEventType[] = [
     'REQUEST_CREATED',
     'REQUEST_UPDATED',
     'REQUEST_ASSIGNED',
     'REQUEST_ASSIGNMENT_CREATED',
     'REQUEST_ASSIGNMENT_ACCEPTED',
     'REQUEST_ASSIGNMENT_REJECTED',
     'REQUEST_REASSIGNED',
     'REQUEST_UNASSIGNED',
     'REQUEST_SCHEDULE_CHANGED',
     'REQUEST_DIAGNOSIS_UPDATED',
     'REQUEST_ATTACHMENTS_CHANGED',
     'REQUEST_STATUS_CHANGED',
     'REQUEST_DELETED'
   ]
   ```

### 7.3 `repairSystem/app/components/ManagerChatBox.vue`
1. **Resolve `messageId` Property Mapping**:
   In `handleRealtime`:
   ```typescript
   function handleRealtime(event: RealtimeEvent) {
     if (event.type === 'CHAT_MESSAGE_CREATED') {
       const payload = event.payload as ChatMessagePayload
       if (payload.conversationId !== conversation.value?.id) return

       const normalizedMessage: ChatMessage = {
         id: payload.messageId,
         conversationId: payload.conversationId,
         senderType: payload.senderType,
         senderId: payload.senderId,
         clientMessageId: payload.clientMessageId || '',
         messageType: payload.messageType,
         text: payload.text || '',
         attachmentId: payload.attachmentId,
         replyToMessageId: payload.replyToMessageId,
         createdAt: payload.createdAt || new Date().toISOString()
       }

       if (!messages.value.some(m => m.id === normalizedMessage.id || (m.clientMessageId && m.clientMessageId === normalizedMessage.clientMessageId))) {
         messages.value.push(normalizedMessage)
         void markRead()
       }
     }
     if (event.type === 'CHAT_MESSAGE_READ') {
       const payload = event.payload as ChatReadPayload
       if (payload.conversationId === conversation.value?.id) {
         // Mark local messages up to payload.messageId as read
       }
     }
     if (event.type === 'CHAT_TYPING_STARTED' || event.type === 'CHAT_TYPING_STOPPED') {
       const payload = event.payload as ChatTypingPayload
       if (payload.conversationId === conversation.value?.id) {
         typing.value = payload.typing
       }
     }
   }
   ```
2. **Send Typing Indicators via STOMP**:
   ```typescript
   client.publish({
     destination: '/app/chat.typing',
     body: JSON.stringify({
       conversationId: conversation.value.id,
       typing: true
     })
   })
   ```

---

## 8. Frontend Verification Checklist

- [ ] STOMP client connects with `Authorization: Bearer <token>` in `connectHeaders` (no arbitrary HTTP handshake headers).
- [ ] Subscribes exclusively to `/user/queue/events` and `/user/queue/chat`.
- [ ] No subscriptions to `/topic/staff.events`.
- [ ] Manager chat box handles incoming messages with `payload.messageId`.
- [ ] Optimistic messages in manager chat box are deduplicated using `clientMessageId`.
- [ ] Request list and dashboard automatically refresh on `REQUEST_*` and `DASHBOARD_INVALIDATED` events.
