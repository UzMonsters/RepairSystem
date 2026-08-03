create table notification_outbox (
    id bigserial primary key,
    event_key varchar(240) not null,
    notification_type varchar(80) not null,
    channel varchar(20) not null,
    recipient_type varchar(20) not null,
    recipient_id bigint not null,
    repair_request_id bigint,
    template_key varchar(120) not null,
    payload_json varchar(4000) not null,
    payload_version integer not null,
    status varchar(30) not null,
    attempt_count integer not null default 0,
    next_attempt_at timestamp with time zone not null,
    processing_started_at timestamp with time zone,
    processing_lease_until timestamp with time zone,
    worker_id varchar(120),
    provider_message_id varchar(120),
    delivered_at timestamp with time zone,
    skipped_at timestamp with time zone,
    dead_at timestamp with time zone,
    last_failure_category varchar(80),
    last_failure_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint notification_outbox_event_key_unique unique (event_key),
    constraint notification_outbox_repair_request_fk foreign key (repair_request_id) references repair_requests(id),
    constraint notification_outbox_type_check check (notification_type in (
        'CUSTOMER_REQUEST_CREATED',
        'CUSTOMER_TECHNICIAN_ASSIGNED',
        'CUSTOMER_TECHNICIAN_REASSIGNED',
        'CUSTOMER_TECHNICIAN_UNASSIGNED',
        'CUSTOMER_VISIT_SCHEDULED',
        'CUSTOMER_VISIT_RESCHEDULED',
        'CUSTOMER_VISIT_SCHEDULE_CLEARED',
        'CUSTOMER_REPAIR_STARTED',
        'CUSTOMER_WAITING_FOR_PARTS',
        'CUSTOMER_REPAIR_RESUMED',
        'CUSTOMER_REPAIR_COMPLETED',
        'CUSTOMER_REQUEST_CANCELLED',
        'TECHNICIAN_NEW_ASSIGNMENT',
        'TECHNICIAN_REASSIGNED_TO_REQUEST',
        'TECHNICIAN_REMOVED_FROM_REQUEST',
        'TECHNICIAN_VISIT_SCHEDULED',
        'TECHNICIAN_VISIT_RESCHEDULED',
        'TECHNICIAN_VISIT_SCHEDULE_CLEARED',
        'TECHNICIAN_REQUEST_CANCELLED'
    )),
    constraint notification_outbox_channel_check check (channel = 'TELEGRAM'),
    constraint notification_outbox_recipient_type_check check (recipient_type in ('CUSTOMER', 'TECHNICIAN')),
    constraint notification_outbox_status_check check (
        status in ('PENDING', 'PROCESSING', 'RETRY_SCHEDULED', 'DELIVERED', 'SKIPPED', 'DEAD')
    ),
    constraint notification_outbox_attempt_count_check check (attempt_count >= 0),
    constraint notification_outbox_payload_version_check check (payload_version = 1),
    constraint notification_outbox_payload_json_check check (
        length(trim(payload_json)) > 0 and length(payload_json) <= 4000
    ),
    constraint notification_outbox_template_key_check check (length(trim(template_key)) > 0),
    constraint notification_outbox_event_key_check check (length(trim(event_key)) > 0),
    constraint notification_outbox_processing_check check (
        (status = 'PROCESSING' and processing_started_at is not null
            and processing_lease_until is not null and worker_id is not null)
        or (status <> 'PROCESSING' and processing_started_at is null
            and processing_lease_until is null and worker_id is null)
    ),
    constraint notification_outbox_terminal_check check (
        (status = 'DELIVERED' and delivered_at is not null and skipped_at is null and dead_at is null)
        or (status = 'SKIPPED' and skipped_at is not null and delivered_at is null and dead_at is null)
        or (status = 'DEAD' and dead_at is not null and delivered_at is null and skipped_at is null)
        or (status in ('PENDING', 'PROCESSING', 'RETRY_SCHEDULED')
            and delivered_at is null and skipped_at is null and dead_at is null)
    ),
    constraint notification_outbox_updated_check check (updated_at >= created_at)
);

create index idx_notification_outbox_status_next_attempt_created
    on notification_outbox(status, next_attempt_at, created_at);
create index idx_notification_outbox_status_processing_lease
    on notification_outbox(status, processing_lease_until);
create index idx_notification_outbox_repair_request
    on notification_outbox(repair_request_id);
create index idx_notification_outbox_recipient
    on notification_outbox(recipient_type, recipient_id);
create index idx_notification_outbox_type
    on notification_outbox(notification_type);

create table notification_delivery_attempts (
    id bigserial primary key,
    notification_id bigint not null,
    attempt_number integer not null,
    worker_id varchar(120) not null,
    started_at timestamp with time zone not null,
    finished_at timestamp with time zone not null,
    outcome varchar(40) not null,
    failure_category varchar(80),
    provider_message_id varchar(120),
    created_at timestamp with time zone not null,
    constraint notification_delivery_attempts_notification_fk
        foreign key (notification_id) references notification_outbox(id) on delete cascade,
    constraint notification_delivery_attempts_attempt_unique unique (notification_id, attempt_number),
    constraint notification_delivery_attempts_attempt_number_check check (attempt_number > 0),
    constraint notification_delivery_attempts_outcome_check check (
        outcome in (
            'DELIVERED',
            'TRANSIENT_FAILURE',
            'PERMANENT_FAILURE',
            'RECIPIENT_UNAVAILABLE',
            'LEASE_RECOVERED'
        )
    ),
    constraint notification_delivery_attempts_finished_check check (finished_at >= started_at)
);

create index idx_notification_delivery_attempts_notification
    on notification_delivery_attempts(notification_id, attempt_number desc);
