create table mobile_refresh_sessions (
    id bigserial primary key,
    actor_type varchar(32) not null,
    customer_id bigint references customers(id) on delete cascade,
    technician_id bigint references technicians(id) on delete cascade,
    token_hash varchar(128) not null,
    token_family_id uuid not null,
    parent_session_id bigint references mobile_refresh_sessions(id) on delete set null,
    replaced_by_session_id bigint references mobile_refresh_sessions(id) on delete set null,
    issued_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    last_used_at timestamp with time zone,
    revoked_at timestamp with time zone,
    revocation_reason varchar(64),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint mobile_refresh_sessions_actor_type_check check (actor_type in ('CUSTOMER', 'TECHNICIAN')),
    constraint mobile_refresh_sessions_ownership_check check (
        (actor_type = 'CUSTOMER' and customer_id is not null and technician_id is null) or
        (actor_type = 'TECHNICIAN' and technician_id is not null and customer_id is null)
    ),
    constraint mobile_refresh_sessions_token_hash_not_blank_check check (length(trim(token_hash)) > 0),
    constraint mobile_refresh_sessions_token_hash_unique unique (token_hash),
    constraint mobile_refresh_sessions_expiry_check check (expires_at > issued_at)
);

create index idx_mobile_refresh_sessions_token_family_id on mobile_refresh_sessions(token_family_id);
create index idx_mobile_refresh_sessions_customer_id on mobile_refresh_sessions(customer_id) where customer_id is not null;
create index idx_mobile_refresh_sessions_technician_id on mobile_refresh_sessions(technician_id) where technician_id is not null;
create index idx_mobile_refresh_sessions_expires_at on mobile_refresh_sessions(expires_at);
create index idx_mobile_refresh_sessions_revoked_at on mobile_refresh_sessions(revoked_at);
