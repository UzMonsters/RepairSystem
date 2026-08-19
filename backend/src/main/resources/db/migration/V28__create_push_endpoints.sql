create table push_endpoints (
    id bigserial primary key,
    owner_type varchar(32) not null,
    staff_user_id bigint references users(id) on delete cascade,
    customer_id bigint references customers(id) on delete cascade,
    technician_id bigint references technicians(id) on delete cascade,
    client_type varchar(64) not null,
    platform varchar(32) not null,
    firebase_app_key varchar(64) not null,
    firebase_installation_id varchar(512) not null,
    app_version varchar(64),
    enabled boolean not null default true,
    last_seen_at timestamp with time zone not null,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint push_endpoints_owner_type_check check (owner_type in ('STAFF', 'CUSTOMER', 'TECHNICIAN')),
    constraint push_endpoints_client_type_check check (client_type in ('ADMIN_WEB', 'CUSTOMER_MOBILE', 'TECHNICIAN_MOBILE')),
    constraint push_endpoints_platform_check check (platform in ('WEB', 'ANDROID', 'IOS')),
    constraint push_endpoints_firebase_app_key_check check (firebase_app_key in ('ADMIN_WEB', 'CUSTOMER_ANDROID', 'CUSTOMER_IOS', 'TECHNICIAN_ANDROID', 'TECHNICIAN_IOS')),
    constraint push_endpoints_owner_attribution_check check (
        (owner_type = 'STAFF' and staff_user_id is not null and customer_id is null and technician_id is null)
        or (owner_type = 'CUSTOMER' and staff_user_id is null and customer_id is not null and technician_id is null)
        or (owner_type = 'TECHNICIAN' and staff_user_id is null and customer_id is null and technician_id is not null)
    ),
    constraint push_endpoints_installation_not_blank_check check (length(trim(firebase_installation_id)) > 0),
    constraint push_endpoints_installation_unique unique (firebase_app_key, firebase_installation_id)
);

create index idx_push_endpoints_staff_user_id_enabled on push_endpoints(staff_user_id, enabled) where staff_user_id is not null;
create index idx_push_endpoints_customer_id_enabled on push_endpoints(customer_id, enabled) where customer_id is not null;
create index idx_push_endpoints_technician_id_enabled on push_endpoints(technician_id, enabled) where technician_id is not null;
create index idx_push_endpoints_last_seen_at on push_endpoints(last_seen_at);
