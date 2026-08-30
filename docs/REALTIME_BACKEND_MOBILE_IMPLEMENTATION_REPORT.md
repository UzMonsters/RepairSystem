# RepairAuto Realtime — Backend + Mobile Implementation Report

## 1. Executive Summary

This report documents the verified, production-ready implementation of the realtime messaging and WebSocket subsystem for the **RepairAuto** platform.

All backend realtime architecture corrections identified in `docs/REALTIME_WEBSOCKET_CURRENT_STATE_AUDIT.md` have been implemented, tested, and validated. The **Flutter Mobile Application** for both **Customer** and **Technician** roles has been integrated with typed domain event processing, event deduplication, token refresh hooks, app lifecycle awareness, and realtime chat. A contract-verified implementation plan (`docs/REALTIME_FRONTEND_IMPLEMENTATION_PLAN.md`) has been produced for the staff web developer.

---

## 2. Backend Realtime Corrections (`backend/`)

### 2.1 Architecture & Event Pipeline
- **Domain Event Decoupling**: Business services (`RepairAssignmentService`, `RepairExecutionService`, `AttachmentService`, `RepairRequestService`, `UserNotificationService`, `ChatService`) publish strongly typed domain events.
- **Strict Transactional Publication Guarantee**:
  - `RealtimeDomainEventListener` uses `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` without fallback execution (`fallbackExecution = false`).
  - Realtime messages are published **strictly after** the database transaction has committed. If a transaction fails or rolls back, zero realtime messages are emitted.
- **Recipient Isolation & Authorization**:
  - `REQUEST_ASSIGNMENT_CREATED`: Sent to Staff & Assigned Technician (isolated from Customer and other technicians).
  - `REQUEST_ASSIGNMENT_ACCEPTED` / `REJECTED`: Sent to Staff, Technician, and Customer.
  - `REQUEST_REASSIGNED`: Sent to Staff, Old Technician, New Technician, and Customer.
  - `REQUEST_UNASSIGNED`: Sent to Staff, Old Technician, and Customer.
  - `REQUEST_SCHEDULE_CHANGED`, `REQUEST_DIAGNOSIS_UPDATED`, `REQUEST_ATTACHMENTS_CHANGED`, `REQUEST_STATUS_CHANGED`, `REQUEST_DELETED`: Sent to Staff, Assigned Technician, and Customer.
  - `CHAT_*` events: Dispatched exclusively to active conversation participants on `/user/queue/chat`.
- **Elimination of Public Broadcasts**:
  - The public `/topic/staff.events` broadcast destination has been removed from backend event publishers.
- **Session Registry & STOMP Broker Delivery**:
  - `RealtimeSessionRegistry` maintains an in-memory mapping between `(ActorType, ActorId)` and active STOMP session IDs, as well as staff role mappings.
  - `StompAuthChannelInterceptor` authenticates JWT tokens on `CONNECT` frames and sets the security `Principal` on `SUBSCRIBE` and `SEND` frames.
  - `SpringWebSocketRealtimeEventPublisher` directly addresses user session queues (`/queue/events-user{sessionId}` and `/queue/chat-user{sessionId}`), guaranteeing deterministic broker delivery.

### 2.2 Deployment Model & Horizontal Scaling Limitations
- **Current Verified Deployment Model**: **Single-Instance Deployment**.
  - The in-memory `SimpleBroker` and in-memory `RealtimeSessionRegistry` operate strictly within a single Spring Boot application instance.
- **Horizontal Scaling Constraints**:
  - If scaled horizontally across multiple instances in the future, cross-instance event distribution will require an external message broker (e.g. RabbitMQ/ActiveMQ via Spring's `StompBrokerRelay`) and a shared distributed session store (e.g. Redis) to route messages across instances.
  - No external broker infrastructure is introduced in this task.

### 2.3 Realtime Event Count
The backend source of truth (`RealtimeEventType.java`) defines **20** event types:
1. `REQUEST_CREATED`
2. `REQUEST_UPDATED`
3. `REQUEST_ASSIGNED`
4. `REQUEST_ASSIGNMENT_CREATED`
5. `REQUEST_ASSIGNMENT_ACCEPTED`
6. `REQUEST_ASSIGNMENT_REJECTED`
7. `REQUEST_REASSIGNED`
8. `REQUEST_UNASSIGNED`
9. `REQUEST_SCHEDULE_CHANGED`
10. `REQUEST_DIAGNOSIS_UPDATED`
11. `REQUEST_ATTACHMENTS_CHANGED`
12. `REQUEST_STATUS_CHANGED`
13. `REQUEST_DELETED`
14. `DASHBOARD_INVALIDATED`
15. `NOTIFICATION_CREATED`
16. `NOTIFICATION_READ`
17. `CHAT_MESSAGE_CREATED`
18. `CHAT_MESSAGE_READ`
19. `CHAT_TYPING_STARTED`
20. `CHAT_TYPING_STOPPED`

### 2.4 Staff Notification Realtime Verification
- **Inspection Result**: `UserNotificationService.java` explicitly skips staff users (`if (event.recipientType() == NotificationRecipientType.STAFF) return RecordResult.SKIPPED;`).
- Staff users do **not** have an inbox entity in the database and do **not** receive `NOTIFICATION_CREATED` or `NOTIFICATION_READ` events over WebSocket.
- Staff web UI relies on REST endpoints and domain event triggers (`REQUEST_*` / `DASHBOARD_INVALIDATED`).

### 2.5 Backend Test Verification
- **Unit Tests**:
  - `RepairAssignmentServiceUnitTest`: 100% Passed.
  - `RealtimeDomainEventListenerTest`: 100% Passed.
  - `SpringWebSocketRealtimeEventPublisherTest`: 100% Passed.
- **Integration Tests**:
  - `RealtimeStompIntegrationTest`: Live multi-actor STOMP integration test suite passed across Customer A, Customer B, Tech A, Tech B, and Staff sessions.
  - Verifies request lifecycle, assignment, reassignment, unassignment, diagnosis updates, soft deletion, chat isolation, and rollback isolation.
- **Full Backend Build**: `./gradlew.bat test` — **100% Passed**.

---

## 3. Flutter Mobile Realtime Integration (`mobile/`)

### 3.1 Typed Realtime Models (`mobile/lib/models.dart`)
- `RealtimeEventType` enum matching all 20 backend event types with graceful fallback for unknown types.
- `RealtimeEnvelope<T>` parser matching `eventId`, `type`, `occurredAt`, and `payload`.
- Strongly typed payloads matching backend DTO definitions:
  - `RequestRealtimePayload`
  - `AssignmentRealtimePayload`
  - `ScheduleRealtimePayload`
  - `DiagnosisRealtimePayload`
  - `AttachmentRealtimePayload`
  - `RequestDeletedRealtimePayload`
  - `NotificationRealtimePayload`
  - `ChatMessageRealtimePayload`
  - `ChatReadRealtimePayload`
  - `ChatTypingRealtimePayload`

### 3.2 Mobile Realtime Client (`mobile/lib/realtime_client.dart`)
- **Dynamic Token Retrieval**: Always acquires the latest access token from `AuthStore` before connection or reconnection.
- **Automatic Reconnection & Token Refresh**: Integrates `reconnectWithToken(String newToken)` and emits `onReconnected` signal.
- **Bounded Deduplication Window**: Implemented bounded LRU deduplication (500 entries) by `eventId`, `messageId`, and composite event keys.
- **Private Queue Subscriptions**: Subscribes to `/user/queue/events` and `/user/queue/chat`.
- **STOMP Sending**: Exposes `send('/app/chat.typing', ...)` for typing indicators.

### 3.3 App Lifecycle & API Client Integration (`mobile/lib/api_client.dart`, `mobile/lib/home_page.dart`)
- Hooked `ApiClient.onTokenRefreshed` to `RealtimeClient.reconnectWithToken`.
- Implemented `WidgetsBindingObserver` in `HomePage` to reconnect and trigger data reconciliation on `AppLifecycleState.resumed`.

### 3.4 Screen Realtime Handlers
- **`CustomerRequests` (`mobile/lib/customer_requests.dart`)**: Filters domain events for `REQUEST_*` lifecycle changes and re-fetches list.
- **`TechnicianJobs` (`mobile/lib/technician_jobs.dart`)**: Filters domain events for assignment and repair workflow changes.
- **`NotificationsScreen` (`mobile/lib/notifications.dart`)**: Filters for `NOTIFICATION_CREATED` and `NOTIFICATION_READ`.
- **`RequestDetails` (`mobile/lib/request_details.dart`)**: Listens to request-specific domain events; handles soft deletion by popping screen with user alert.
- **`JobActions` (`mobile/lib/job_actions.dart`)**: Passes realtime client context to chat and details.
- **`MobileChatScreen` (`mobile/lib/mobile_chat.dart`)**:
  - Optimistic message rendering with pending/sent/failed indicators.
  - Reconciles optimistic messages with server response using `clientMessageId`.
  - Deduplicates incoming messages by `messageId`.
  - Listens to `CHAT_MESSAGE_READ` and updates read receipts in realtime.
  - Displays debounced typing indicator for other participants and emits typing events via STOMP.
  - Re-syncs message history on reconnection.

### 3.5 Mobile Verification
- `flutter analyze` in `mobile/`: **No issues found (0 warnings, 0 errors)**.
- `flutter test` in `mobile/`: **21 tests passed (100% Passed)**.

---

## 4. Staff Web Frontend Contract Summary (`repairSystem/`)

Per ownership boundaries, no code in `repairSystem/` was modified. The final frontend implementation plan has been authored and published at:
`docs/REALTIME_FRONTEND_IMPLEMENTATION_PLAN.md`

Summary of contract points for the frontend developer:
1. **Authentication**: Connect using the STOMP `CONNECT` frame header `Authorization: Bearer <token>` (browser native WebSockets do not send custom HTTP handshake headers).
2. **Subscriptions**: Subscribe exclusively to `/user/queue/events` and `/user/queue/chat`. Remove deprecated `/topic/staff.events` subscription.
3. **Payload Property Mapping**: In `ManagerChatBox.vue`, read `payload.messageId` (numeric ID) instead of expecting `payload.id`.
4. **Notifications**: Do not subscribe `NavNotifications.vue` to `NOTIFICATION_*` events (staff notifications use REST/dashboard invalidation).

---

## 5. Artifact Summary

| File | Status | Description |
| :--- | :--- | :--- |
| `backend/src/main/java/.../realtime/session/RealtimeSessionRegistry.java` | Created/Updated | Maps sessions by actor & staff role |
| `backend/src/main/java/.../realtime/auth/StompAuthChannelInterceptor.java` | Created/Updated | Authenticates JWT & sets Principal |
| `backend/src/main/java/.../realtime/delivery/SpringWebSocketRealtimeEventPublisher.java` | Created/Updated | Delivers to user session queue endpoints |
| `backend/src/main/java/.../realtime/event/application/RealtimeDomainEventListener.java` | Updated | Transactional event listener without fallbackExecution |
| `backend/src/test/java/.../realtime/RealtimeStompIntegrationTest.java` | Created | Full multi-actor STOMP integration test suite |
| `mobile/lib/models.dart` | Updated | Typed realtime DTOs (20 event types, `occurredAt`) |
| `mobile/lib/realtime_client.dart` | Updated | Resilient STOMP client with dedup & reconnection |
| `mobile/lib/api_client.dart` | Updated | Token refresh callback integration |
| `mobile/lib/home_page.dart` | Updated | App lifecycle resume & reconnection |
| `mobile/lib/customer_requests.dart` | Updated | Customer domain event filtering |
| `mobile/lib/technician_jobs.dart` | Updated | Technician domain event filtering |
| `mobile/lib/notifications.dart` | Updated | Notification event filtering |
| `mobile/lib/request_details.dart` | Updated | Request lifecycle & deletion handling |
| `mobile/lib/job_actions.dart` | Updated | Job actions chat routing |
| `mobile/lib/mobile_chat.dart` | Updated | Optimistic chat, read receipts & typing |
| `mobile/test/realtime_test.dart` | Created | Unit tests for models & realtime client |
| `docs/REALTIME_FRONTEND_IMPLEMENTATION_PLAN.md` | Updated | Web staff frontend implementation plan |
| `docs/REALTIME_BACKEND_MOBILE_IMPLEMENTATION_REPORT.md` | Updated | Complete implementation report |
