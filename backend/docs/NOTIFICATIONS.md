# Notifications

Phase 9 implements Telegram-only transactional notifications through a durable PostgreSQL outbox.

## Reliability Model

Business services insert notification rows in the same transaction as the committed repair event.
The notification worker then claims eligible rows with PostgreSQL row locking, commits that claim,
sends through `TelegramBotClient` outside the business transaction, and finalizes delivery in a short transaction.

Delivery is durable at-least-once with idempotent outbox creation. Each business event and recipient has a stable
`event_key`, and PostgreSQL uniqueness remains the final duplicate guard.

Telegram does not provide a general message idempotency key. If Telegram accepts a message but the backend loses the
response before recording `DELIVERED`, a retry can send a duplicate message. The worker bounds retries and records
append-only delivery attempts, but it does not claim perfect exactly-once Telegram delivery.

## Recipient Policy

Customer notifications are delivered only when the customer exists, is active, and has a Telegram chat id.
Technician notifications are delivered only when the technician exists, is active, and has a Telegram chat id.
Unavailable recipients are marked `SKIPPED` with `RECIPIENT_UNAVAILABLE`.

## Scope

Only Telegram transactional notifications are supported. Email, SMS, push notifications, reviews, dashboards,
pricing, payments, and marketing or bulk messaging are not part of Phase 9.
