create table notification_push_deliveries (
    id bigserial primary key,
    notification_outbox_id bigint not null references notification_outbox(id) on delete cascade,
    push_endpoint_id bigint not null references push_endpoints(id) on delete cascade,
    status varchar(30) not null,
    attempt_count integer not null default 0,
    firebase_message_id varchar(255),
    next_attempt_at timestamp with time zone not null,
    delivered_at timestamp with time zone,
    dead_at timestamp with time zone,
    last_error_code varchar(80),
    last_error_category varchar(80),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint notification_push_deliveries_status_check check (
        status in ('PENDING', 'PROCESSING', 'RETRY_SCHEDULED', 'DELIVERED', 'SKIPPED', 'DEAD')
    ),
    constraint notification_push_deliveries_attempt_count_check check (attempt_count >= 0),
    constraint notification_push_deliveries_unique unique (notification_outbox_id, push_endpoint_id)
);

create index idx_push_deliveries_outbox_id on notification_push_deliveries(notification_outbox_id);
create index idx_push_deliveries_endpoint_id on notification_push_deliveries(push_endpoint_id);
create index idx_push_deliveries_status_next_attempt on notification_push_deliveries(status, next_attempt_at);

alter table notification_outbox drop constraint if exists notification_outbox_recipient_type_check;
alter table notification_outbox
    add constraint notification_outbox_recipient_type_check check (recipient_type in ('CUSTOMER', 'TECHNICIAN', 'STAFF'));
