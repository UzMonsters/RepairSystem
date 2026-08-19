-- V31__harden_user_notification_idempotency.sql
-- Step 14.1: Inbox Idempotency & Partial Unique Index Hardening

-- 1. Drop the multi-column unique constraint that allowed duplicate rows due to NULL semantics
alter table user_notifications drop constraint if exists user_notifications_unique_event_recipient;

-- 2. Create authoritative partial unique indexes per recipient type
create unique index idx_user_notifications_unique_customer
on user_notifications(event_key, customer_id)
where recipient_type = 'CUSTOMER';

create unique index idx_user_notifications_unique_technician
on user_notifications(event_key, technician_id)
where recipient_type = 'TECHNICIAN';

-- 3. Create index for read notifications filtering
create index idx_user_notifications_customer_read on user_notifications(customer_id, read_at) where read_at is not null;
create index idx_user_notifications_technician_read on user_notifications(technician_id, read_at) where read_at is not null;
