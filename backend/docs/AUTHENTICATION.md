# Authentication And User Management

## Bootstrap Admin

The backend can create the first administrator at startup when no admin exists.
Bootstrap is disabled by default.

Required environment variables when enabled:

```text
APP_BOOTSTRAP_ADMIN_ENABLED=true
APP_BOOTSTRAP_ADMIN_EMAIL=<admin-email>
APP_BOOTSTRAP_ADMIN_PASSWORD=<admin-password>
APP_BOOTSTRAP_ADMIN_FULL_NAME=System Administrator
```

The bootstrap password must pass the same password policy as normal user
creation. Disable bootstrap after initial setup.

Bootstrap uses a PostgreSQL transaction-level advisory lock with key
`834645201180001`. Startup flow acquires the lock, re-checks whether an
administrator exists, and creates exactly one admin when none exists. The lock
protects multiple application instances starting against the same database.

## Password Policy

Passwords must be 10 to 128 characters and include uppercase, lowercase, digit,
and non-alphanumeric characters. Passwords cannot equal the user's email.

Passwords are hashed with BCrypt and are never returned by API responses.

## Tokens

Login returns a short-lived JWT access token and an opaque refresh token.

- Access token TTL defaults to `PT15M`.
- Refresh token TTL defaults to `P30D`.
- Access tokens are signed with HS256 using `APP_JWT_SECRET`.
- `APP_JWT_SECRET` must contain at least 32 characters.
- Refresh tokens are stored only as SHA-256 hashes.
- Refresh tokens rotate on every successful refresh.
- Reusing an already-used refresh token revokes the full token family.
- Access tokens include an `authVersion` claim.
- The request authentication filter compares the JWT `authVersion` with the
  current database user. A mismatch returns `INVALID_ACCESS_TOKEN`.

`authVersion` starts at `1` and is incremented atomically in the database when
security-sensitive state changes:

- Password change
- Role change
- User deactivation
- Admin `POST /api/v1/users/{id}/revoke-sessions`
- Authenticated `POST /api/v1/auth/logout-all`

Reactivation does not reset `authVersion`, so access tokens issued before
deactivation remain invalid. Single-session logout revokes only the supplied
refresh token and does not invalidate already-issued access tokens.

Refresh sessions are revoked through persistence. Access tokens are invalidated
through `authVersion`.

For unknown-email login, the service performs one BCrypt match against a
precomputed dummy hash before returning the generic `INVALID_CREDENTIALS`
response. This is timing-hardening, not a claim of perfect constant-time
behavior.

## Roles

Supported roles:

- `ADMIN`
- `MANAGER`

`ADMIN` can manage users. `MANAGER` can authenticate and manage Phase 2
customers and technicians, but cannot access `/api/v1/users/**`. Category write
operations remain admin-only.

The backend prevents disabling or demoting the last active admin and prevents an
admin from disabling their own account.

## Endpoint Summary

Public:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

Authenticated:

- `GET /api/v1/auth/me`
- `PATCH /api/v1/auth/password`
- `POST /api/v1/auth/logout-all`

Admin-only:

- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `POST /api/v1/users`
- `PUT /api/v1/users/{id}`
- `PATCH /api/v1/users/{id}/role`
- `PATCH /api/v1/users/{id}/activation`
- `POST /api/v1/users/{id}/revoke-sessions`
