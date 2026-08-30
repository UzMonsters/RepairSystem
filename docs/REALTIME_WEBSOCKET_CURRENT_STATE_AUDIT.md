# RepairAuto Realtime / WebSocket Current-State Audit

Date: 2026-08-29

## 1. Executive Summary

Overall assessment: **PARTIALLY BROKEN**

The backend has a coherent STOMP/WebSocket foundation: `/ws`, `/app`, `/queue`, `/topic`, `/user`, STOMP `CONNECT` JWT authentication, user queues for domain/chat events, and `@TransactionalEventListener(phase = AFTER_COMMIT)` for persisted domain events. The historical chat routing cleanup is mostly present: chat events are classified and sent only to `/user/queue/chat`, while non-chat events go to `/user/queue/events`.

The broken behavior is mostly in coverage and client synchronization rather than the socket endpoint itself:

- Assignment lifecycle events do not distinguish initial assignment from acceptance at the public realtime event type level. Both emit `REQUEST_ASSIGNED`.
- Schedule/reschedule/clear-schedule, diagnosis updates, request deletion, and attachment upload/delete do not publish realtime domain events.
- Staff receives duplicate request/dashboard events because `publishToStaff()` sends to every staff user queue and also broadcasts the same event to `/topic/staff.events`, while staff web subscribes to both.
- Staff chat receives backend `CHAT_MESSAGE_CREATED` payloads shaped with `messageId`, but the active chat component checks `payload.id`, so incoming websocket chat messages are not appended.
- Mobile request/job list screens refetch on every event, but open mobile request detail and mobile chat screens do not subscribe to realtime at all.
- Tests currently do not prove real end-to-end STOMP delivery. The focused test run could not execute because test compilation fails in an unrelated notification test.

## 2. Current Architecture

Backend:

- WebSocket config: `backend/src/main/java/com/example/darks/repair_auto/realtime/config/WebSocketConfig.java`
- Endpoint: `/ws`, registered once as native WebSocket and once with SockJS fallback.
- Allowed origins: `*` via `setAllowedOriginPatterns("*")`.
- Application prefix: `/app`.
- User destination prefix: `/user`.
- Simple in-memory broker: `/queue`, `/topic`.
- Inbound channel interceptor: `StompAuthChannelInterceptor`.

Authentication:

- HTTP `/ws` is permitted by Spring Security, then STOMP `CONNECT` is authenticated by `StompAuthChannelInterceptor`.
- Supported auth headers: STOMP native `Authorization`, lowercase `authorization`, or STOMP passcode.
- Staff tokens resolve to `AuthenticatedUser`; `Principal.getName()` is staff email.
- Customer/technician tokens resolve to `AuthenticatedMobileActor`; `Principal.getName()` is phone/identifier.

Publication:

- Business services publish Spring application events inside transactions.
- `RealtimeDomainEventListener` handles persisted events with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
- `SpringWebSocketRealtimeEventPublisher` sends:
  - chat events to `/user/queue/chat`;
  - non-chat events to `/user/queue/events`;
  - staff-wide events to both user queues and `/topic/staff.events`.

Clients:

- Staff web: `repairSystem/app/composables/useRealtime.ts`, global Nuxt plugin `repairSystem/app/plugins/realtime.client.ts`.
- Customer/technician mobile: `mobile/lib/realtime_client.dart`, created in `mobile/lib/home_page.dart`.

## 3. Event Inventory

| Event | Produced By | Intended Recipient | Actual Destination | Staff Web | Customer Mobile | Technician Mobile | UI Effect | Status |
| -- | -- | -- | -- | -- | -- | -- | -- | -- |
| `REQUEST_CREATED` | `RepairRequestService.create`, `telegramCreate`, `mobileCreate` -> `RequestCreatedDomainEvent` | Staff, customer | Staff: `/user/queue/events` and `/topic/staff.events`; customer: `/user/queue/events` | Global request list/dashboard refetch | Request list refetches because it listens to all events | Not relevant unless received | Lists/dashboard update; customer list updates | Works with duplicate staff delivery |
| `REQUEST_UPDATED` | `RepairRequestService.update` -> `RequestUpdatedDomainEvent` | Staff, customer | Staff: user queue + topic; customer user queue | List refetch; open matching detail refetch | List refetches | Not relevant | Staff/customer lists update | Partial |
| `REQUEST_ASSIGNED` | Initial assignment/reassignment: `RequestAssignmentCreatedDomainEvent`; acceptance: `RequestAssignedDomainEvent` | Initial assign: staff + assigned tech. Acceptance: staff + customer + tech | Initial assign: staff user queue + topic, tech user queue. Acceptance: staff user queue + topic, customer user queue, tech user queue | List/detail/dashboard refetch | List refetches when customer receives acceptance only | Job list refetches | State refresh but semantics ambiguous | Defective semantics |
| `REQUEST_UNASSIGNED` | Unassign/reject -> `RequestUnassignedDomainEvent` | Staff, technician; customer for unassign/reject depending business intent | Staff user queue + topic, technician user queue. Current listener does not publish to customer even when payload has customerId | Staff list/detail/dashboard refetch | No event for customer | Job list refetches | Customer may remain stale until manual/list reload from another source | Partial |
| `REQUEST_STATUS_CHANGED` | Start, wait-for-parts, resume, complete, cancel -> `RequestStatusChangedDomainEvent` | Staff, customer, technician when assigned | Staff user queue + topic, customer user queue, technician user queue if tech id present | List/detail/dashboard refetch | List refetches | Job list refetches | Status changes update lists; open mobile detail does not | Partial |
| `DASHBOARD_INVALIDATED` | `RealtimeDomainEventListener` after request create/assign/unassign/status; direct `DashboardInvalidatedDomainEvent` handler exists | Staff | Staff user queue + `/topic/staff.events` | Dashboard data refetch | Not subscribed/produced to mobile | Not subscribed/produced to mobile | Staff dashboard refetch | Works with duplicate delivery |
| `NOTIFICATION_CREATED` | `UserNotificationService.record` for customer/technician inbox records | Customer/technician notification recipient | Recipient `/user/queue/events` | Staff topbar listens, but no staff producer found | Notifications screen refetches | Notifications screen refetches | Mobile notifications update | Mobile only in current code |
| `NOTIFICATION_READ` | `UserNotificationService.markAsRead`, `markAllAsRead` | Customer/technician | Recipient `/user/queue/events` | Staff topbar listens, but no staff producer found | Notifications screen refetches | Notifications screen refetches | Mobile notifications update | Mobile only in current code |
| `CHAT_MESSAGE_CREATED` | `ChatService.sendMessage` -> `ChatMessageCreatedDomainEvent` | Sender and active participants | `/user/queue/chat` | Active manager chat attempts append but checks wrong payload field | No active mobile chat subscription | No active mobile chat subscription | Staff active chat misses websocket append; mobile chat stale | Defective client handling |
| `CHAT_MESSAGE_READ` | `ChatService.markAsRead` -> `ChatMessageReadDomainEvent` | Other participants | `/user/queue/chat` | No visible read handler found | No active mobile chat subscription | No active mobile chat subscription | Read receipts not surfaced in inspected clients | Mostly unused |
| `CHAT_TYPING_STARTED` | `ChatService.handleTyping`, STOMP `/app/chat.typing` | Other participants | `/user/queue/chat` | Active manager chat toggles typing | No mobile handler | No mobile handler | Staff typing can show if payload conversation matches | Partial |
| `CHAT_TYPING_STOPPED` | `ChatService.handleTyping`, STOMP `/app/chat.typing` | Other participants | `/user/queue/chat` | Active manager chat clears typing | No mobile handler | No mobile handler | Staff typing can clear | Partial |

Dead/unused or underused definitions:

- `RequestAssignedDomainEvent` represents acceptance, but the external realtime type is still `REQUEST_ASSIGNED`.
- `RequestAssignmentCreatedDomainEvent` represents assignment creation/reassignment, but the external realtime type is also `REQUEST_ASSIGNED`.
- Staff web listens for `NOTIFICATION_CREATED`/`NOTIFICATION_READ`, but inspected producers only create these events from the mobile inbox service.
- `CHAT_MESSAGE_READ` is produced but no inspected staff/mobile UI updates read state from it.

## 4. Client Subscription Matrix

| Client | Subscriptions | Start Time | Reconnect | Cleanup | Notes |
| -- | -- | -- | -- | -- | -- |
| Staff web | `/topic/staff.events`, `/user/queue/events`, `/user/queue/chat` | `plugins/realtime.client.ts` watches authenticated state | STOMP reconnect every 5s; `useAuth.refreshSession()` calls `useRealtime().reconnect()` after REST token refresh | Logout disconnects; listeners remove on component unmount | Duplicate request/dashboard events because topic and user queue both carry staff events |
| Customer mobile | `/user/queue/events`, `/user/queue/chat` | `HomePage.initState()` | STOMP reconnect every 5s with original token captured at connect | `HomePage.dispose()` disposes service | List and notifications screens listen; detail/chat screens do not |
| Technician mobile | `/user/queue/events`, `/user/queue/chat` | `HomePage.initState()` | STOMP reconnect every 5s with original token captured at connect | `HomePage.dispose()` disposes service | Job and notification lists listen; action sheet/details/chat do not |

## 5. Confirmed Defects

### RT-001

Severity: **HIGH**  
Component: Assignment lifecycle semantics

Current behavior: Initial assignment/reassignment and assignment acceptance both reach clients as `REQUEST_ASSIGNED`. `RepairAssignmentService.assign()` and `reassign()` publish `RequestAssignmentCreatedDomainEvent`; `RealtimeDomainEventListener.handleRequestAssignmentCreated()` emits `RealtimeEventType.REQUEST_ASSIGNED`. `RepairAssignmentService.accept()` and `acceptByTechnician()` publish `RequestAssignedDomainEvent`; `handleRequestAssigned()` also emits `REQUEST_ASSIGNED`.

Expected behavior: Clients should be able to distinguish assignment-created from technician-accepted state, or reliably refetch enough authoritative data without interpreting assignment-created as accepted.

Root cause: Two different domain states are collapsed into one realtime event type and payload status (`"ASSIGNED"`).

Evidence:

- `backend/src/main/java/com/example/darks/repair_auto/repair/assignment/application/RepairAssignmentService.java:163`
- `backend/src/main/java/com/example/darks/repair_auto/repair/assignment/application/RepairAssignmentService.java:283`
- `backend/src/main/java/com/example/darks/repair_auto/realtime/event/application/RealtimeDomainEventListener.java:77`
- `backend/src/main/java/com/example/darks/repair_auto/realtime/event/application/RealtimeDomainEventListener.java:100`

Affected users: Staff, customers, technicians.

Recommended direction: Separate public realtime semantics for assignment-created vs assignment-accepted, or include unambiguous assignment status/action metadata and make clients treat it strictly as invalidation.

### RT-002

Severity: **HIGH**  
Component: Staff chat UI

Current behavior: Backend `CHAT_MESSAGE_CREATED` payload uses `messageId`, but `ManagerChatBox.vue` checks `payload.id`. Incoming websocket chat messages therefore fail the append condition.

Expected behavior: Active staff chat should append the authoritative persisted message exactly once.

Root cause: Payload contract mismatch between backend DTO and frontend `ChatMessage` shape.

Evidence:

- Backend payload field: `backend/src/main/java/com/example/darks/repair_auto/realtime/event/dto/ChatMessagePayload.java:6`
- Frontend check: `repairSystem/app/components/ManagerChatBox.vue:97`

Affected users: Staff using technician-manager chat.

Recommended direction: Normalize realtime chat payload into the frontend `ChatMessage` shape or handle `messageId` explicitly.

### RT-003

Severity: **HIGH**  
Component: Schedule realtime coverage

Current behavior: `RepairAssignmentService.schedule()` mutates assignment schedule and possibly request status, records history, and enqueues notifications, but publishes no realtime event.

Expected behavior: Schedule/reschedule/clear-schedule should invalidate staff request lists/details, customer request lists/details, technician jobs/schedule, and dashboards where relevant.

Root cause: Missing realtime publication after schedule mutation.

Evidence:

- Schedule mutation without publish: `backend/src/main/java/com/example/darks/repair_auto/repair/assignment/application/RepairAssignmentService.java:245`
- Notification-only schedule helper: `backend/src/main/java/com/example/darks/repair_auto/repair/assignment/application/RepairAssignmentService.java:587`

Affected users: Staff, customers, technicians.

Recommended direction: Publish an after-commit request/schedule invalidation event to affected parties.

### RT-004

Severity: **HIGH**  
Component: Diagnosis realtime coverage

Current behavior: `RepairExecutionService.updateDiagnosis()` and `updateDiagnosisByTechnician()` update execution details and return REST data to the caller, but publish no realtime event.

Expected behavior: Open request detail/execution panels should refresh when diagnosis changes.

Root cause: Missing realtime publication for non-status execution detail mutation.

Evidence:

- Staff diagnosis update path returns saved execution without publish: `backend/src/main/java/com/example/darks/repair_auto/repair/execution/application/RepairExecutionService.java:231`
- Technician diagnosis update path returns saved execution without publish: `backend/src/main/java/com/example/darks/repair_auto/repair/execution/application/RepairExecutionService.java:270`

Affected users: Staff and technicians viewing the request/job; customers if diagnosis is exposed.

Recommended direction: Add an execution/request-updated invalidation signal, or broaden `REQUEST_UPDATED` semantics to cover execution detail changes.

### RT-005

Severity: **MEDIUM**  
Component: Staff duplicate delivery

Current behavior: `publishToStaff()` sends to all active staff user destinations and then broadcasts the same event to `/topic/staff.events`. Staff web subscribes to both destinations, so active staff sessions receive duplicate request/dashboard events.

Expected behavior: Each staff client should have one authoritative delivery path for a given event, or handlers should deduplicate by event id.

Root cause: Redundant staff user-scoped and topic-scoped publication with no client dedupe.

Evidence:

- Staff user queue plus topic send: `backend/src/main/java/com/example/darks/repair_auto/realtime/delivery/SpringWebSocketRealtimeEventPublisher.java:61`
- Staff web subscribes to both: `repairSystem/app/composables/useRealtime.ts:80`

Affected users: Staff web.

Recommended direction: Choose a single staff delivery contract per event class or dedupe by `eventId` in the client event bus.

### RT-006

Severity: **MEDIUM**  
Component: Customer notification on unassign/reject

Current behavior: `RequestUnassignedDomainEvent` includes `customerId`, but `handleRequestUnassigned()` publishes only to staff and technician. Customer notification outbox entries can exist for manager unassign, but there is no direct request realtime invalidation to the customer.

Expected behavior: If customer-facing request state changes from assigned/scheduled to new, customer request views should be invalidated directly.

Root cause: Listener omits `publishToUser(ActorType.CUSTOMER, ...)` in the unassigned handler.

Evidence:

- Event has customer id in payload: `backend/src/main/java/com/example/darks/repair_auto/realtime/event/application/RealtimeDomainEventListener.java:129`
- Handler publishes staff and technician only: `backend/src/main/java/com/example/darks/repair_auto/realtime/event/application/RealtimeDomainEventListener.java:138`

Affected users: Customers.

Recommended direction: Align unassign/reject recipients with customer-visible REST state changes.

### RT-007

Severity: **MEDIUM**  
Component: Mobile active screens

Current behavior: Customer request list, technician jobs list, and notifications screen listen to the shared realtime stream and refetch on any event. `RequestDetails` and `MobileChatScreen` do not receive or subscribe to realtime events, so open detail/chat screens remain stale.

Expected behavior: Open mobile detail and chat screens should refresh or update when relevant events arrive.

Root cause: Realtime stream is passed only to list/notification screens, not detail/chat screens.

Evidence:

- Stream passed to list/notification screens: `mobile/lib/home_page.dart:56`
- Customer list listens to all events: `mobile/lib/customer_requests.dart:29`
- Request detail has no event stream field/listener: `mobile/lib/request_details.dart:3`
- Mobile chat has no event stream field/listener: `mobile/lib/mobile_chat.dart:3`

Affected users: Customer and technician mobile users.

Recommended direction: Propagate realtime stream to detail/chat screens and filter by request/conversation id.

### RT-008

Severity: **MEDIUM**  
Component: Request deletion and attachment realtime coverage

Current behavior: Staff request soft-delete and attachment upload/delete do not publish realtime events. Uploading/deleting attachments refreshes only the caller's current screen.

Expected behavior: Request deletion and attachment changes should invalidate relevant lists/details for staff and affected mobile actors.

Root cause: Missing event publication in request soft-delete and attachment service mutation paths.

Evidence:

- Soft delete without publish: `backend/src/main/java/com/example/darks/repair_auto/repair/request/application/RepairRequestService.java:416`
- Attachment upload paths enter `uploadStream()` without realtime publication: `backend/src/main/java/com/example/darks/repair_auto/repair/attachment/application/AttachmentService.java:117`
- Attachment delete marks deleted without realtime publication: `backend/src/main/java/com/example/darks/repair_auto/repair/attachment/application/AttachmentService.java:285`

Affected users: Staff, customers, technicians.

Recommended direction: Add attachment/request invalidation events for affected request participants.

### RT-009

Severity: **MEDIUM**  
Component: Mobile reconnect/token refresh

Current behavior: `MobileRealtimeClient.connect()` reads the access token once and configures reconnect with that token. `ApiClient` can refresh REST tokens on 401, but there is no hook to reconnect STOMP with the refreshed token.

Expected behavior: Reconnect after token expiry/auth failure should use the latest access token.

Root cause: REST refresh and realtime lifecycle are not coordinated in mobile.

Evidence:

- Token captured at connect: `mobile/lib/realtime_client.dart:20`
- STOMP reconnect enabled with fixed headers: `mobile/lib/realtime_client.dart:32`
- REST refresh stores new token independently: `mobile/lib/api_client.dart:112`

Affected users: Customer and technician mobile users after token refresh or reconnect.

Recommended direction: On mobile token refresh, disconnect/reconnect realtime with the updated token.

### RT-010

Severity: **LOW**  
Component: Production configuration

Current behavior: Repository production config points frontend/mobile realtime defaults at `wss://repair-auto.onrender.com/ws`, while `render.yaml` backend service name is `repair-system-backend` and `NUXT_BACKEND_URL` points to `https://repair-system-backend.onrender.com`.

Expected behavior: REST and realtime production hostnames should target the same deployed backend unless an alias is intentionally configured outside the repo.

Root cause: Hostname drift in checked-in config.

Evidence:

- Nuxt realtime default: `repairSystem/nuxt.config.ts:52`
- Render backend URL: `render.yaml:107`
- Render realtime URL: `render.yaml:109`
- Mobile API default: `mobile/lib/api_client.dart:11`

Affected users: Staff web and mobile users in production-like deployments.

Recommended direction: Verify the actual production hostname and make config consistent.

## 6. Suspected / Unverified Defects

| ID | Severity | Component | Suspicion | Confidence |
| -- | -- | -- | -- | -- |
| RT-S01 | MEDIUM | Staff notification topbar | Staff topbar listens for notification realtime events, but no staff `NotificationCreatedDomainEvent` producer was found. It may update only through manual/page reloads. | HIGHLY LIKELY |
| RT-S02 | MEDIUM | Staff topic authorization | `/topic/staff.events` broadcasts to all staff regardless of role-specific REST visibility. Current REST gives ADMIN and MANAGER broad request/dashboard access, so no immediate violation was proven. | POSSIBLE |
| RT-S03 | MEDIUM | SockJS auth | Backend supports SockJS, but inspected staff/mobile clients use native WebSocket STOMP. SockJS fallback auth behavior was not end-to-end tested. | POSSIBLE |
| RT-S04 | LOW | Subscription duplication | STOMP clients subscribe in `onConnect`. Libraries usually clear subscriptions on reconnect, but no test proves subscriptions cannot duplicate after reconnect edge cases. | POSSIBLE |
| RT-S05 | LOW | Event ordering | Events include `occurredAt` but no aggregate version/revision. Out-of-order delivery could cause unnecessary or stale refetch ordering in edge cases. | POSSIBLE |

## 7. Request Lifecycle Audit

| Operation | Produced Event | Who Is Actually Notified | Staff UI | Customer Mobile | Technician Mobile | Gaps |
| -- | -- | -- | -- | -- | -- | -- |
| Request created | `REQUEST_CREATED` + staff `DASHBOARD_INVALIDATED` | Staff, customer | List/dashboard refetch | List refetch | N/A | Staff duplicate delivery |
| Request updated | `REQUEST_UPDATED` | Staff, customer | List/detail refetch | List refetch | N/A | Dashboard invalidation not explicit |
| Request assigned | `REQUEST_ASSIGNED` from `RequestAssignmentCreatedDomainEvent` | Staff, assigned technician | List/detail/dashboard refetch | Not notified directly | Jobs list refetch | Customer does not get assignment-created event by current code |
| Technician accepted | `REQUEST_ASSIGNED` from `RequestAssignedDomainEvent` | Staff, customer, technician | List/detail/dashboard refetch | List refetch | Jobs list refetch | Event type same as assignment-created |
| Technician rejected | `REQUEST_UNASSIGNED` | Staff, technician | List/detail/dashboard refetch | No direct request event | Jobs list refetch | Customer state can be stale |
| Request reassigned | `REQUEST_ASSIGNED` for new tech only | Staff, new technician | List/detail/dashboard refetch | Not notified directly | New tech jobs refetch | Old technician gets no realtime request event; chat participant is updated silently |
| Request unassigned | `REQUEST_UNASSIGNED` | Staff, old technician | List/detail/dashboard refetch | No direct request event | Jobs list refetch | Customer state can be stale |
| Scheduled | None | No realtime request event | Caller only via REST response | Notification may later appear, no request event | Notification may later appear, no job/schedule event | Missing event |
| Rescheduled | None | No realtime request event | Caller only via REST response | Notification may later appear | Notification may later appear | Missing event |
| Schedule cleared | None | No realtime request event | Caller only via REST response | Notification may later appear | Notification may later appear | Missing event |
| Work started | `REQUEST_STATUS_CHANGED` | Staff, customer, technician | List/detail/dashboard refetch | List refetch | Jobs list refetch | Open mobile detail stale |
| Diagnosis updated | None | No realtime event | Caller only via REST response | No update | Caller only if technician made change | Missing event |
| Waiting for parts | `REQUEST_STATUS_CHANGED` | Staff, customer, technician | List/detail/dashboard refetch | List refetch | Jobs list refetch | Open mobile detail stale |
| Resumed | `REQUEST_STATUS_CHANGED` | Staff, customer, technician | List/detail/dashboard refetch | List refetch | Jobs list refetch | Open mobile detail stale |
| Completed | `REQUEST_STATUS_CHANGED` | Staff, customer, technician | List/detail/dashboard refetch | List refetch | Jobs list refetch | Open mobile detail stale; mobile review affordance may remain based on old item |
| Cancelled | `REQUEST_STATUS_CHANGED` | Staff, customer, technician if assigned | List/detail/dashboard refetch | List refetch | Jobs list refetch | Open mobile detail stale |
| Attachments changed | None | No realtime event | Caller refreshes attachment panel only | Caller gets snackbar only | Caller closes sheet only | Missing event |
| Request deleted | None | No realtime event | Caller navigates/list refreshes only | No update | No update | Missing event |

## 8. Assignment Lifecycle Audit

Assignment created:

- Backend method: `RepairAssignmentService.assign()`.
- Domain event: `RequestAssignmentCreatedDomainEvent`.
- Realtime type: `REQUEST_ASSIGNED`.
- Recipients: staff and assigned technician.
- Customer is not directly notified by realtime assignment-created event.
- Dashboard invalidated for staff.

Technician accepts:

- Backend methods: `accept()` and `acceptByTechnician()`.
- Domain event: `RequestAssignedDomainEvent`.
- Realtime type: `REQUEST_ASSIGNED`.
- Recipients: staff, customer, technician.
- Customer is notified only at acceptance, which is good for avoiding premature acceptance state, but the event name does not distinguish acceptance from assignment.

Technician rejects:

- Backend methods: `reject()` and `rejectByTechnician()`.
- Domain event: `RequestUnassignedDomainEvent`.
- Realtime type: `REQUEST_UNASSIGNED`.
- Recipients: staff and technician. The event payload carries `customerId`, but the listener does not send it to customer.
- Staff notification outbox entries are created through `notifyStaffAssignmentRejected()`.

Reassign:

- Old technician is removed from chat participants by `ChatService.handleTechnicianReassigned()`.
- New technician receives `REQUEST_ASSIGNED`.
- Old technician does not receive a request realtime event for removal, though notification outbox gets `TECHNICIAN_UNASSIGNED`.

## 9. Chat Audit

Routing:

- `CHAT_MESSAGE_CREATED`, `CHAT_MESSAGE_READ`, `CHAT_TYPING_STARTED`, and `CHAT_TYPING_STOPPED` are classified as chat events in `SpringWebSocketRealtimeEventPublisher`.
- Chat events are sent to `/user/queue/chat`.
- Publisher tests verify chat events are not also sent to `/user/queue/events`.

Message created:

- `ChatService.sendMessage()` persists the message, advances sender read state, publishes `ChatMessageCreatedDomainEvent`.
- `RealtimeDomainEventListener.handleChatMessageCreated()` publishes to sender once and to active recipients excluding sender duplicates.
- This satisfies the historical no-mirroring contract at backend publisher level.

Client behavior:

- Staff active chat handler has a payload mismatch and does not append backend realtime messages because it checks `payload.id` while backend sends `messageId`.
- Mobile chat does not subscribe to realtime events, so sender echo and receiver delivery are not realtime-visible in open chat.
- `CHAT_MESSAGE_READ` is produced but no inspected UI visibly handles read receipts.
- Typing can reach staff manager chat, but mobile chat has no typing send/receive UI path.

## 10. Authentication & User Destination Audit

Principal identities:

| Actor | Backend principal class | `Principal.getName()` | Publisher targeting |
| -- | -- | -- | -- |
| Staff | `AuthenticatedUser` inside `UsernamePasswordAuthenticationToken` | Email | `ActorType.STAFF`, staff user id -> session registry -> principal email |
| Customer | `AuthenticatedMobileActor` | Phone/identifier | `ActorType.CUSTOMER`, customer id -> session registry -> principal phone |
| Technician | `AuthenticatedMobileActor` | Phone/identifier | `ActorType.TECHNICIAN`, technician id -> session registry -> principal phone |

Important behavior:

- Direct fallback in `publishToUser()` uses `actorType:id` if no session registry entry exists. That fallback does not match current staff email or mobile phone principals when tokens are created with phone/email identifiers.
- Therefore, successful delivery to active users depends on `RealtimeSessionEventListener` registering sessions correctly.
- Staff `CONNECT` test proves `Principal.getName()` equals email.
- No test proved a real connected client receives a `convertAndSendToUser()` event after session registration.

Authorization:

- `/user/**` subscriptions are allowed for authenticated users.
- `/topic/staff...` is allowed only when principal is staff.
- Direct `/topic/**` and `/queue/**` subscriptions are rejected otherwise.
- STOMP `SEND` is limited to `/app/**`.

## 11. Frontend State Synchronization Audit

Staff web:

- Global request events call `refreshNuxtData(['requests-list', 'dashboard-recent'])` and debounced dashboard refresh.
- Open request detail listens for `REQUEST_UPDATED`, `REQUEST_ASSIGNED`, `REQUEST_UNASSIGNED`, and `REQUEST_STATUS_CHANGED` matching `payload.requestId`, then refreshes request, assignments, attachments, status history, and execution.
- Detail page does not receive events for schedule-only, diagnosis-only, attachment-only, or delete mutations because backend does not publish them.
- Notification topbar reloads on `NOTIFICATION_CREATED`/`NOTIFICATION_READ`, but staff producers were not found.
- Manager chat active component misses created messages due to `messageId`/`id` mismatch.

Mobile:

- Customer request list refetches on every realtime event without filtering.
- Technician jobs list refetches on every realtime event without filtering.
- Notifications screen refetches on every realtime event without filtering.
- Request detail and chat screens do not participate in realtime updates.

## 12. Mobile Realtime Audit

Connection:

- `MobileRealtimeClient` builds WebSocket URL from `apiBaseUrl` and appends `/ws`.
- It sends token in both STOMP connect headers and websocket connect headers.
- It subscribes to `/user/queue/events` and `/user/queue/chat`.

Lifecycle:

- A single realtime client is owned by `HomePage`.
- Disposed on `HomePage.dispose()`.
- No app lifecycle observer handles background/foreground.
- No REST reconciliation is run on reconnect inside the realtime client.

State:

- List-level screens refetch broadly.
- Active detail/chat state remains stale.

## 13. Reconnect / Recovery Audit

Staff web:

- STOMP reconnect delay: 5 seconds.
- On connect, it resubscribes and refetches request/dashboard data.
- On REST token refresh, `useAuth.refreshSession()` calls `useRealtime().reconnect()`, which reads the updated access cookie.
- No event-id dedupe exists, so staff topic/user-queue duplicates and reconnect duplicate-subscription edge cases are not defended in the event bus.

Mobile:

- STOMP reconnect delay: 5 seconds.
- Subscriptions are re-created in `onConnect`.
- No explicit REST reconciliation after reconnect.
- No token-refresh integration.
- Reconnect uses the original headers captured during `connect()`.

## 14. Security Findings

No confirmed cross-customer or cross-technician delivery bug was found in the inspected publisher path. User-specific delivery maps actor type/id through the active session registry before calling `convertAndSendToUser()`.

Security risks:

- `setAllowedOriginPatterns("*")` allows any browser origin to attempt a STOMP connection. Authentication is still required, but this is broad for production.
- `/topic/staff.events` broadcasts to every staff principal. Current REST permissions for request/dashboard data are broad for ADMIN/MANAGER, but this should remain aligned with REST authorization if roles become narrower.
- The simple broker/session registry is in-memory. Multiple backend instances would not share session state or broker messages.

## 15. Test Coverage Assessment

Existing tests inspected:

- `SpringWebSocketRealtimeEventPublisherTest`
  - Verifies chat events go to `/queue/chat`, not `/queue/events`.
  - Verifies non-chat events go to `/queue/events`, not `/queue/chat`.
  - Verifies staff events also go to `/topic/staff.events`.
  - Uses mocks; does not prove real STOMP delivery.
- `RealtimeDomainEventIntegrationTest`
  - Invokes listener methods directly and verifies publisher calls.
  - Does not prove transaction-after-commit behavior from actual service method through committed DB state to connected client.
- `WebSocketAuthIntegrationTest`
  - Verifies token authentication and subscribe rejection for customer -> staff topic.
  - Does not open a real websocket client subscription and receive a user-destination message.
- `ChatStompControllerTest`
  - Verifies STOMP controller principal resolution delegates to `ChatService`.
  - Does not prove broker delivery or frontend handling.

Command run:

```powershell
.\gradlew.bat test --tests "com.example.darks.repair_auto.realtime.*" --tests "com.example.darks.repair_auto.chat.api.ChatStompControllerTest"
```

Result:

- Failed before executing focused tests.
- `:compileTestJava` fails in `backend/src/test/java/com/example/darks/repair_auto/notification/NotificationIntegrationTest.java:433` with `illegal start of expression`.

Coverage gaps:

- No verified real STOMP connect/subscribe/receive flow.
- No wrong-user non-delivery test.
- No reconnect or duplicate-delivery test.
- No request lifecycle end-to-end realtime test.
- No frontend/mobile event handling tests for stale UI behavior.
- No test covering assignment-created vs acceptance semantics.

## 16. Production Infrastructure Risks

- Backend uses Spring's simple in-memory broker. It is acceptable for one backend instance, but multiple backend instances would break cross-instance websocket delivery unless an external broker or sticky single-instance assumption is enforced.
- Render config currently shows one backend web service on the free plan, so multi-instance breakage is architectural risk rather than a confirmed current production fault.
- Frontend and mobile realtime defaults point to `repair-auto.onrender.com`, while Render backend service is `repair-system-backend.onrender.com`. This needs production verification.
- SockJS is enabled, but inspected clients use native websocket STOMP. SockJS fallback behavior is unverified.
- CORS/origin for STOMP endpoint is broad (`*`).

## 17. Root-Cause Summary

| ID | Severity | Root Cause | Affected Flow | Confidence |
| -- | -- | -- | -- | -- |
| RT-001 | HIGH | Assignment-created and assignment-accepted collapse into `REQUEST_ASSIGNED` | Assignment, acceptance | CONFIRMED |
| RT-002 | HIGH | Backend chat payload uses `messageId`; staff UI checks `id` | Staff active chat | CONFIRMED |
| RT-003 | HIGH | No realtime event after schedule mutation | Schedule/reschedule/clear | CONFIRMED |
| RT-004 | HIGH | No realtime event after diagnosis mutation | Diagnosis update | CONFIRMED |
| RT-005 | MEDIUM | Staff events sent to both staff user queues and staff topic | Staff lists/dashboard/detail | CONFIRMED |
| RT-006 | MEDIUM | Unassigned handler omits customer delivery | Reject/unassign customer state | CONFIRMED |
| RT-007 | MEDIUM | Mobile detail/chat screens do not subscribe to realtime stream | Mobile details/chat | CONFIRMED |
| RT-008 | MEDIUM | Delete/attachment mutations publish no realtime events | Request delete, attachments | CONFIRMED |
| RT-009 | MEDIUM | Mobile REST token refresh does not reconnect STOMP with new token | Mobile reconnect/auth recovery | HIGHLY LIKELY |
| RT-010 | LOW | Checked-in production realtime/backend hostnames differ | Production connectivity | POSSIBLE |

## 18. Recommended Remediation Order

1. Fix the staff chat payload mismatch so delivered chat messages are visible.
2. Clarify assignment realtime semantics so created/accepted/rejected are distinguishable or safely treated as invalidation.
3. Add missing realtime coverage for schedule, diagnosis, attachments, request deletion, and customer unassign/reject invalidation.
4. Remove or deduplicate staff duplicate delivery between `/topic/staff.events` and `/user/queue/events`.
5. Wire mobile detail and chat screens to realtime, with targeted refetch by request/conversation id.
6. Coordinate mobile token refresh with realtime reconnect and add REST reconciliation after reconnect.
7. Add end-to-end STOMP tests for authenticated connect, user destination receipt, wrong-user non-receipt, request lifecycle routing, chat routing, and reconnect behavior after fixing the current test compilation blocker.
8. Verify production realtime hostname/origin configuration and document the single-instance/simple-broker limitation.
