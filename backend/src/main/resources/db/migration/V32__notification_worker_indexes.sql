-- Step 16: Production notification worker and push endpoint performance indexes

create index if not exists idx_notification_outbox_channel_status_claim
    on notification_outbox(channel, status, next_attempt_at, created_at);

create index if not exists idx_notification_outbox_channel_lease
    on notification_outbox(channel, status, processing_lease_until);

create index if not exists idx_push_endpoints_enabled_last_seen
    on push_endpoints(enabled, last_seen_at)
    where enabled = true;
