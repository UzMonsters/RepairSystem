create table refresh_sessions (
    id bigserial primary key,
    user_id bigint not null references users(id),
    token_hash varchar(128) not null,
    token_family_id uuid not null,
    issued_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    revoked_at timestamp with time zone,
    replaced_by_token_id bigint references refresh_sessions(id) on delete set null,
    revocation_reason varchar(120),
    created_ip varchar(64),
    created_user_agent varchar(512),
    last_used_ip varchar(64),
    last_used_user_agent varchar(512),
    created_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint refresh_sessions_token_hash_not_blank_check check (length(trim(token_hash)) > 0),
    constraint refresh_sessions_expiry_check check (expires_at > issued_at),
    constraint refresh_sessions_revocation_reason_check check (
        revocation_reason is null or length(trim(revocation_reason)) > 0
    )
);
