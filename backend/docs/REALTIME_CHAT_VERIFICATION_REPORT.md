# Realtime Chat Verification Report

Verified against the current Java source, Flyway migration, Spring Security configuration, DTO records, and integration tests.

## Mismatches Corrected

- REST paths: the backend exposes `/api/v1/mobile/me/conversations` and `/api/v1/conversations`; older references to `/api/v1/mobile/chat` or `/api/v1/staff/conversations` are not implemented.
- Conversation types: the backend supports `CUSTOMER_TECHNICIAN` and `TECHNICIAN_MANAGER`. There is no three-party customer + technician + manager group chat type.
- Message types: the backend supports `TEXT`, `IMAGE`, and `FILE`. `DOCUMENT`, `LOCATION`, and `SYSTEM` are not part of the implemented contract.
- Attachments: each chat message has a singular `attachmentId`; multiple selected files must be sent as multiple messages.
- Read state: `unread_count` is not persisted. `last_read_message_id` is stored and unread counts are computed when conversations are listed or fetched.
- Event payloads: request, notification, dashboard, chat message, read, and typing examples now match the actual DTO records.
- Request number examples: examples now use the production `REP-{year}-{sequence}` format.
- FCM policy: the backend does not track active foreground conversation subscriptions for push suppression. Chat push is sent to all enabled recipient endpoints except the sender when Firebase push is enabled.

## Implementation Defects Fixed

- Mobile request conversation lookup now authorizes the mobile actor before creating a `CUSTOMER_TECHNICIAN` conversation.
- Reassigned or removed technicians can no longer fetch messages created after their `leftAt` timestamp through REST history or mark those messages as read.
- `CHAT_MESSAGE_CREATED` is no longer delivered twice to the sender when the recipient list contains the sender.
- Chat FCM push dispatch now runs from the after-commit chat domain event instead of inside the send transaction.
- Flyway V36 now enforces Java enum values for conversation type/status, participant actor type, and message sender/message type, and adds uniqueness for request/type conversations.

## Verification Added

- Regression coverage for unauthorized mobile conversation creation side effects.
- Regression coverage for reassigned technician history cutoff.
- Regression coverage for duplicate sender realtime delivery.

## Realtime Channel & Reassignment Hardening (Final Cleanup)

- **Channel Separation**: Removed duplicate chat event mirroring to `/user/queue/events`.
  - `/user/queue/chat`: Carries `CHAT_*` events exclusively (`CHAT_MESSAGE_CREATED`, `CHAT_MESSAGE_READ`, `CHAT_TYPING_STARTED`, `CHAT_TYPING_STOPPED`).
  - `/user/queue/events`: Carries user-scoped non-chat domain events (`REQUEST_*`, `NOTIFICATION_*`, `DASHBOARD_INVALIDATED`).
  - `/topic/staff.events`: Staff broadcast topic for system-wide request and dashboard invalidations.
- **Technician Reassignment Contract**:
  - Removed technician retains access only to messages created at or before `leftAt` and receives no future realtime chat events.
  - Newly assigned technician receives access to the existing request conversation history (including messages exchanged before assignment) and receives future messages normally.
- **Tests Added**:
  - `SpringWebSocketRealtimeEventPublisherTest`: Verifies exclusive routing between `/queue/chat` and `/queue/events` without duplicate mirroring.
  - `ChatIntegrationTest.technicianReassignment_historyAndCutoffBehavior`: Verifies complete reassignment lifecycle (old technician cutoff at `leftAt`, new technician full historical access, rejection of sends from removed technician).

