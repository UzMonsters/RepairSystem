-- V30__create_user_notifications.sql
-- Step 14: Mobile In-App Notification Inbox & Read/Unread State

create table user_notifications (
    id bigserial primary key,
    event_key varchar(255) not null,
    notification_type varchar(50) not null,
    recipient_type varchar(30) not null,
    customer_id bigint references customers(id) on delete cascade,
    technician_id bigint references technicians(id) on delete cascade,
    repair_request_id bigint references repair_requests(id) on delete set null,
    request_number varchar(64),
    target varchar(64) not null,
    target_id bigint,
    payload_json text not null,
    read_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint user_notifications_recipient_type_check check (
        recipient_type in ('CUSTOMER', 'TECHNICIAN')
    ),
    constraint user_notifications_customer_recipient_check check (
        (recipient_type = 'CUSTOMER' and customer_id is not null and technician_id is null) or
        (recipient_type = 'TECHNICIAN' and customer_id is null and technician_id is not null)
    ),
    constraint user_notifications_unique_event_recipient unique (event_key, recipient_type, customer_id, technician_id)
);

create index idx_user_notifications_customer_created on user_notifications(customer_id, created_at desc, id desc);
create index idx_user_notifications_customer_unread on user_notifications(customer_id, read_at) where read_at is null;
create index idx_user_notifications_technician_created on user_notifications(technician_id, created_at desc, id desc);
create index idx_user_notifications_technician_unread on user_notifications(technician_id, read_at) where read_at is null;
