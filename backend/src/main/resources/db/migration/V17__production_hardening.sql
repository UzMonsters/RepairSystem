create table auth_throttle_entries (
    id bigserial primary key,
    throttle_key varchar(160) not null,
    failed_attempts integer not null,
    window_started_at timestamp with time zone not null,
    blocked_until timestamp with time zone,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint auth_throttle_entries_key_unique unique (throttle_key),
    constraint auth_throttle_entries_failed_attempts_check check (failed_attempts >= 0),
    constraint auth_throttle_entries_updated_check check (updated_at >= window_started_at)
);

create index idx_auth_throttle_entries_blocked_until
    on auth_throttle_entries(blocked_until)
    where blocked_until is not null;

create index idx_auth_throttle_entries_updated_at
    on auth_throttle_entries(updated_at);

alter table repair_attachments
    add column object_purged_at timestamp with time zone;

create index idx_repair_attachments_stale_uploading
    on repair_attachments(status, uploaded_at, id)
    where status = 'UPLOADING';

create index idx_repair_attachments_deleted_cleanup
    on repair_attachments(status, deleted_at, id)
    where status = 'DELETED' and object_purged_at is null;

create index idx_telegram_updates_status_received_at
    on telegram_updates(status, received_at);

create index idx_notification_delivery_attempts_created_at
    on notification_delivery_attempts(created_at);
