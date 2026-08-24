alter table customers
    add column auth_version bigint not null default 0,
    add column email varchar(254),
    add column email_verified_at timestamp with time zone,
    add column phone_verified_at timestamp with time zone;

alter table customers
    alter column phone drop not null;

alter table customers
    drop constraint if exists customers_phone_format_check;

alter table customers
    add constraint customers_phone_format_check check (
        phone is null or phone ~ '^\+998[0-9]{9}$'
    );

alter table customers
    drop constraint if exists customers_registration_source_check;

alter table customers
    add constraint customers_registration_source_check check (
        registration_source in ('ADMIN', 'TELEGRAM', 'GOOGLE', 'PHONE')
    );

alter table customers
    add constraint customers_email_format_check check (
        email is null or (length(trim(email)) > 0 and length(email) <= 254)
    );

create index idx_customers_email_lower on customers(lower(email)) where email is not null;
create index idx_customers_auth_version on customers(auth_version);

alter table technicians
    add column auth_version bigint not null default 0,
    add column email varchar(254),
    add column email_verified_at timestamp with time zone,
    add column phone_verified_at timestamp with time zone;

alter table technicians
    add constraint technicians_email_format_check check (
        email is null or (length(trim(email)) > 0 and length(email) <= 254)
    );

create unique index technicians_active_email_unique
    on technicians(lower(email))
    where email is not null and active = true;

create index idx_technicians_email_lower on technicians(lower(email)) where email is not null;
create index idx_technicians_auth_version on technicians(auth_version);

create table mobile_auth_identities (
    id bigserial primary key,
    actor_type varchar(32) not null,
    customer_id bigint references customers(id) on delete cascade,
    technician_id bigint references technicians(id) on delete cascade,
    provider varchar(32) not null,
    provider_subject varchar(255) not null,
    provider_email varchar(254),
    provider_phone varchar(32),
    verified_at timestamp with time zone not null,
    linked_at timestamp with time zone not null default now(),
    last_used_at timestamp with time zone,
    disabled_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint mobile_auth_identities_actor_type_check check (actor_type in ('CUSTOMER', 'TECHNICIAN')),
    constraint mobile_auth_identities_provider_check check (provider in ('TELEGRAM', 'GOOGLE', 'PHONE')),
    constraint mobile_auth_identities_ownership_check check (
        (actor_type = 'CUSTOMER' and customer_id is not null and technician_id is null) or
        (actor_type = 'TECHNICIAN' and technician_id is not null and customer_id is null)
    ),
    constraint mobile_auth_identities_subject_not_blank_check check (length(trim(provider_subject)) > 0)
);

create unique index mobile_auth_identities_provider_subject_unique
    on mobile_auth_identities(actor_type, provider, provider_subject)
    where disabled_at is null;

create unique index mobile_auth_identities_customer_provider_unique
    on mobile_auth_identities(customer_id, provider)
    where customer_id is not null and disabled_at is null;

create unique index mobile_auth_identities_technician_provider_unique
    on mobile_auth_identities(technician_id, provider)
    where technician_id is not null and disabled_at is null;

create index idx_mobile_auth_identities_customer_id on mobile_auth_identities(customer_id) where customer_id is not null;
create index idx_mobile_auth_identities_technician_id on mobile_auth_identities(technician_id) where technician_id is not null;
create index idx_mobile_auth_identities_provider_email on mobile_auth_identities(lower(provider_email)) where provider_email is not null;
create index idx_mobile_auth_identities_provider_phone on mobile_auth_identities(provider_phone) where provider_phone is not null;

insert into mobile_auth_identities (
    actor_type,
    customer_id,
    provider,
    provider_subject,
    provider_phone,
    verified_at,
    linked_at,
    last_used_at,
    created_at,
    updated_at
)
select
    'CUSTOMER',
    id,
    'TELEGRAM',
    telegram_user_id::text,
    phone,
    coalesce(updated_at, created_at, now()),
    coalesce(updated_at, created_at, now()),
    null,
    coalesce(created_at, now()),
    coalesce(updated_at, created_at, now())
from customers
where telegram_user_id is not null
on conflict do nothing;

insert into mobile_auth_identities (
    actor_type,
    technician_id,
    provider,
    provider_subject,
    provider_phone,
    verified_at,
    linked_at,
    last_used_at,
    created_at,
    updated_at
)
select
    'TECHNICIAN',
    id,
    'TELEGRAM',
    telegram_user_id::text,
    phone,
    coalesce(telegram_linked_at, updated_at, created_at, now()),
    coalesce(telegram_linked_at, updated_at, created_at, now()),
    null,
    coalesce(created_at, now()),
    coalesce(updated_at, created_at, now())
from technicians
where telegram_user_id is not null
on conflict do nothing;

create table mobile_sessions (
    id uuid primary key,
    actor_type varchar(32) not null,
    customer_id bigint references customers(id) on delete cascade,
    technician_id bigint references technicians(id) on delete cascade,
    client_type varchar(32) not null,
    authentication_provider varchar(32) not null,
    platform varchar(16) not null,
    device_id varchar(128),
    device_name varchar(160),
    app_version varchar(64),
    created_at timestamp with time zone not null default now(),
    last_seen_at timestamp with time zone not null default now(),
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    revocation_reason varchar(64),
    created_ip varchar(64),
    last_ip varchar(64),
    user_agent varchar(512),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint mobile_sessions_actor_type_check check (actor_type in ('CUSTOMER', 'TECHNICIAN')),
    constraint mobile_sessions_provider_check check (authentication_provider in ('TELEGRAM', 'GOOGLE', 'PHONE')),
    constraint mobile_sessions_client_type_check check (client_type in ('CUSTOMER_MOBILE', 'TECHNICIAN_MOBILE')),
    constraint mobile_sessions_platform_check check (platform in ('ANDROID', 'IOS')),
    constraint mobile_sessions_ownership_check check (
        (actor_type = 'CUSTOMER' and customer_id is not null and technician_id is null and client_type = 'CUSTOMER_MOBILE') or
        (actor_type = 'TECHNICIAN' and technician_id is not null and customer_id is null and client_type = 'TECHNICIAN_MOBILE')
    ),
    constraint mobile_sessions_expiry_check check (expires_at > created_at)
);

create index idx_mobile_sessions_customer_id on mobile_sessions(customer_id) where customer_id is not null;
create index idx_mobile_sessions_technician_id on mobile_sessions(technician_id) where technician_id is not null;
create index idx_mobile_sessions_expires_at on mobile_sessions(expires_at);
create index idx_mobile_sessions_revoked_at on mobile_sessions(revoked_at);
create index idx_mobile_sessions_device_id on mobile_sessions(device_id) where device_id is not null;

alter table mobile_refresh_sessions
    add column mobile_session_id uuid references mobile_sessions(id) on delete cascade;

insert into mobile_sessions (
    id,
    actor_type,
    customer_id,
    technician_id,
    client_type,
    authentication_provider,
    platform,
    created_at,
    last_seen_at,
    expires_at,
    revoked_at,
    revocation_reason,
    updated_at
)
select
    gen_random_uuid(),
    actor_type,
    customer_id,
    technician_id,
    case actor_type when 'CUSTOMER' then 'CUSTOMER_MOBILE' else 'TECHNICIAN_MOBILE' end,
    'TELEGRAM',
    'ANDROID',
    min(created_at),
    max(coalesce(last_used_at, created_at)),
    max(expires_at),
    case when bool_and(revoked_at is not null) then max(revoked_at) else null end,
    case when bool_and(revoked_at is not null) then max(revocation_reason) else null end,
    max(updated_at)
from mobile_refresh_sessions
where mobile_session_id is null
group by token_family_id, actor_type, customer_id, technician_id;

with backfilled as (
    select
        r.id as refresh_id,
        s.id as session_id
    from mobile_refresh_sessions r
    join mobile_sessions s
      on s.actor_type = r.actor_type
     and coalesce(s.customer_id, -1) = coalesce(r.customer_id, -1)
     and coalesce(s.technician_id, -1) = coalesce(r.technician_id, -1)
     and s.created_at <= r.created_at
     and s.expires_at >= r.expires_at
    where r.mobile_session_id is null
)
update mobile_refresh_sessions r
set mobile_session_id = backfilled.session_id
from backfilled
where r.id = backfilled.refresh_id;

create index idx_mobile_refresh_sessions_mobile_session_id on mobile_refresh_sessions(mobile_session_id);

create table phone_otp_challenges (
    id uuid primary key,
    phone varchar(32) not null,
    actor_type varchar(32) not null,
    client_type varchar(32) not null,
    purpose varchar(64) not null,
    code_hash varchar(128) not null,
    attempt_count integer not null default 0,
    max_attempts integer not null default 5,
    created_at timestamp with time zone not null default now(),
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    resend_available_at timestamp with time zone not null,
    request_ip varchar(64),
    user_agent varchar(512),
    created_session_id uuid,
    version bigint not null default 0,
    constraint phone_otp_actor_type_check check (actor_type in ('CUSTOMER', 'TECHNICIAN')),
    constraint phone_otp_client_type_check check (client_type in ('CUSTOMER_MOBILE', 'TECHNICIAN_MOBILE')),
    constraint phone_otp_purpose_check check (purpose in ('CUSTOMER_REGISTER_OR_LOGIN', 'TECHNICIAN_LOGIN', 'LINK_PHONE', 'CHANGE_PHONE')),
    constraint phone_otp_attempt_count_check check (attempt_count >= 0 and max_attempts > 0),
    constraint phone_otp_expiry_check check (expires_at > created_at)
);

create index idx_phone_otp_phone on phone_otp_challenges(phone);
create index idx_phone_otp_expires_at on phone_otp_challenges(expires_at);
create index idx_phone_otp_consumed_at on phone_otp_challenges(consumed_at);

create table email_verification_challenges (
    id uuid primary key,
    actor_type varchar(32) not null,
    customer_id bigint references customers(id) on delete cascade,
    technician_id bigint references technicians(id) on delete cascade,
    pending_email varchar(254) not null,
    purpose varchar(64) not null,
    code_hash varchar(128) not null,
    attempt_count integer not null default 0,
    max_attempts integer not null default 5,
    created_at timestamp with time zone not null default now(),
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    resend_available_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint email_verification_actor_type_check check (actor_type in ('CUSTOMER', 'TECHNICIAN')),
    constraint email_verification_ownership_check check (
        (actor_type = 'CUSTOMER' and customer_id is not null and technician_id is null) or
        (actor_type = 'TECHNICIAN' and technician_id is not null and customer_id is null)
    ),
    constraint email_verification_purpose_check check (purpose in ('ADD_EMAIL', 'CHANGE_EMAIL')),
    constraint email_verification_attempt_count_check check (attempt_count >= 0 and max_attempts > 0),
    constraint email_verification_expiry_check check (expires_at > created_at)
);

create index idx_email_verification_customer_id on email_verification_challenges(customer_id) where customer_id is not null;
create index idx_email_verification_technician_id on email_verification_challenges(technician_id) where technician_id is not null;
create index idx_email_verification_email on email_verification_challenges(lower(pending_email));
