# Security

## Authentication

The backend uses stateless JWT access tokens and rotating refresh tokens. Refresh tokens are stored only as hashes. Refresh reuse detection revokes the token family. Password changes and explicit logout-all increment authorization state so stale tokens stop working.

## Abuse Controls

`POST /api/v1/auth/login` and `POST /api/v1/auth/refresh` are protected by database-backed throttling. Throttle keys are SHA-256 hashes of scope, client IP, and credential material. The policy is multi-instance safe and returns `429 AUTH_THROTTLED` after repeated failures.

Ingress-level rate limits are still required for production. Recommended minimums:

- Authentication: low burst, low sustained rate per IP.
- Telegram webhook: provider IP allowlist where possible, low burst, bounded body size.
- Attachment upload: strict body size limit matching application config.

## Public Route Policy

Only authentication, Telegram webhook, health/info, and optional OpenAPI routes are public. All business routes require JWT authorization.

## Telegram Webhook

The webhook requires `X-Telegram-Bot-Api-Secret-Token`, uses constant-time comparison, bounds body size, rejects disabled Telegram configuration, and processes updates through persistent idempotency.

## Headers And CORS

Production rejects wildcard CORS origins and allows only configured HTTPS origins. Responses include no-sniff, no-referrer, cache-control, and HSTS headers where applicable. The API is stateless and must not create `JSESSIONID`.

## Secret Handling

Never commit `.env`, secret files, keystores, database dumps, raw Telegram payloads, or storage directories. Rotate secrets through deployment configuration, then restart instances in a controlled rollout.

Rotating JWT signing secret invalidates existing access tokens. Existing refresh sessions remain valid until used, unless sessions are explicitly revoked.
