alter table repair_requests
    alter column created_by_user_id drop not null;

alter table repair_requests
    add column source_reference varchar(120);

alter table repair_requests
    add constraint repair_requests_source_reference_unique unique (source_reference);

alter table repair_requests
    add constraint repair_requests_source_attribution_check check (
        (source = 'ADMIN' and created_by_user_id is not null and source_reference is null)
        or (source = 'TELEGRAM' and created_by_user_id is null and source_reference is not null)
    );

create index idx_repair_requests_source_reference on repair_requests(source_reference);

alter table repair_attachments
    alter column uploaded_by_user_id drop not null;

alter table repair_attachments
    add column uploaded_by_customer_id bigint;

alter table repair_attachments
    add constraint repair_attachments_uploaded_by_customer_fk
        foreign key (uploaded_by_customer_id) references customers(id);

alter table repair_attachments
    add constraint repair_attachments_uploader_check check (
        (uploaded_by_user_id is not null and uploaded_by_customer_id is null)
        or (uploaded_by_user_id is null and uploaded_by_customer_id is not null)
    );

alter table repair_attachments
    drop constraint repair_attachments_deleted_check;

alter table repair_attachments
    add constraint repair_attachments_deleted_check check (
        (status = 'DELETED' and deleted_at is not null)
        or status <> 'DELETED'
    );

create index idx_repair_attachments_uploaded_by_customer on repair_attachments(uploaded_by_customer_id);

create table telegram_updates (
    id bigserial primary key,
    telegram_update_id bigint not null,
    status varchar(20) not null,
    update_type varchar(40) not null,
    received_at timestamp with time zone not null,
    processed_at timestamp with time zone,
    failure_category varchar(80),
    attempt_count integer not null default 1,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint telegram_updates_update_id_unique unique (telegram_update_id),
    constraint telegram_updates_status_check check (status in ('RECEIVED', 'PROCESSED', 'FAILED')),
    constraint telegram_updates_attempt_count_check check (attempt_count > 0),
    constraint telegram_updates_failure_length_check check (
        failure_category is null or length(trim(failure_category)) between 1 and 80
    ),
    constraint telegram_updates_processed_check check (
        (status = 'PROCESSED' and processed_at is not null)
        or status <> 'PROCESSED'
    ),
    constraint telegram_updates_updated_check check (updated_at >= created_at)
);

create index idx_telegram_updates_status on telegram_updates(status);
create index idx_telegram_updates_received_at on telegram_updates(received_at);

create table telegram_customer_sessions (
    id bigserial primary key,
    telegram_user_id bigint not null,
    telegram_chat_id bigint not null,
    customer_id bigint,
    language varchar(8) not null,
    state varchar(40) not null,
    draft_full_name varchar(160),
    draft_category_id bigint,
    draft_description varchar(2000),
    draft_address varchar(500),
    draft_latitude numeric(9,6),
    draft_longitude numeric(10,6),
    draft_photo_file_ids text,
    created_request_id bigint,
    history_page integer not null default 0,
    last_interaction_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint telegram_customer_sessions_user_unique unique (telegram_user_id),
    constraint telegram_customer_sessions_chat_unique unique (telegram_chat_id),
    constraint telegram_customer_sessions_customer_fk foreign key (customer_id) references customers(id),
    constraint telegram_customer_sessions_draft_category_fk foreign key (draft_category_id) references repair_categories(id),
    constraint telegram_customer_sessions_created_request_fk foreign key (created_request_id) references repair_requests(id),
    constraint telegram_customer_sessions_language_check check (language in ('EN', 'RU', 'UZ')),
    constraint telegram_customer_sessions_state_check check (state in (
        'LANGUAGE_SELECTION',
        'AWAITING_NAME',
        'AWAITING_CONTACT',
        'MAIN_MENU',
        'SELECTING_CATEGORY',
        'AWAITING_DESCRIPTION',
        'AWAITING_PHOTO_OR_SKIP',
        'AWAITING_LOCATION',
        'CONFIRMING_REQUEST',
        'UPDATING_PROFILE_NAME',
        'UPDATING_PROFILE_PHONE'
    )),
    constraint telegram_customer_sessions_history_page_check check (history_page >= 0),
    constraint telegram_customer_sessions_updated_check check (updated_at >= created_at)
);

create index idx_telegram_customer_sessions_customer_id on telegram_customer_sessions(customer_id);
create index idx_telegram_customer_sessions_last_interaction on telegram_customer_sessions(last_interaction_at);
