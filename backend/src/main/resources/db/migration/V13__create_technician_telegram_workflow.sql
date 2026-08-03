alter table repair_executions
    add column started_by_technician_id bigint,
    add column diagnosis_updated_by_technician_id bigint,
    add column completed_by_technician_id bigint,
    add column cancelled_by_technician_id bigint;

alter table repair_executions
    add constraint repair_executions_started_by_technician_fk foreign key (started_by_technician_id) references technicians(id),
    add constraint repair_executions_diagnosis_updated_by_technician_fk foreign key (diagnosis_updated_by_technician_id) references technicians(id),
    add constraint repair_executions_completed_by_technician_fk foreign key (completed_by_technician_id) references technicians(id),
    add constraint repair_executions_cancelled_by_technician_fk foreign key (cancelled_by_technician_id) references technicians(id);

alter table repair_executions
    drop constraint repair_executions_started_user_pair_check,
    drop constraint repair_executions_diagnosis_update_pair_check,
    drop constraint repair_executions_completion_pair_check,
    drop constraint repair_executions_cancellation_pair_check;

alter table repair_executions
    add constraint repair_executions_started_actor_pair_check check (
        (started_at is null and started_by_user_id is null and started_by_technician_id is null)
        or (started_at is not null and (
            (started_by_user_id is not null and started_by_technician_id is null)
            or (started_by_user_id is null and started_by_technician_id is not null)
        ))
    ),
    add constraint repair_executions_diagnosis_update_actor_pair_check check (
        (diagnosis is null and diagnosis_updated_at is null
            and diagnosis_updated_by_user_id is null and diagnosis_updated_by_technician_id is null)
        or (diagnosis is not null and diagnosis_updated_at is not null and (
            (diagnosis_updated_by_user_id is not null and diagnosis_updated_by_technician_id is null)
            or (diagnosis_updated_by_user_id is null and diagnosis_updated_by_technician_id is not null)
        ))
    ),
    add constraint repair_executions_completion_actor_pair_check check (
        (completed_at is null and completed_by_user_id is null and completed_by_technician_id is null)
        or (completed_at is not null and work_performed is not null and (
            (completed_by_user_id is not null and completed_by_technician_id is null)
            or (completed_by_user_id is null and completed_by_technician_id is not null)
        ))
    ),
    add constraint repair_executions_cancellation_actor_pair_check check (
        (cancelled_at is null and cancelled_by_user_id is null and cancelled_by_technician_id is null
            and cancellation_reason is null)
        or (cancelled_at is not null and cancellation_reason is not null and (
            (cancelled_by_user_id is not null and cancelled_by_technician_id is null)
            or (cancelled_by_user_id is null and cancelled_by_technician_id is not null)
        ))
    );

create index idx_repair_executions_started_by_technician on repair_executions(started_by_technician_id);
create index idx_repair_executions_diagnosis_by_technician on repair_executions(diagnosis_updated_by_technician_id);
create index idx_repair_executions_completed_by_technician on repair_executions(completed_by_technician_id);
create index idx_repair_executions_cancelled_by_technician on repair_executions(cancelled_by_technician_id);

alter table repair_request_status_history
    add column changed_by_technician_id bigint;

alter table repair_request_status_history
    add constraint repair_request_status_history_changed_by_technician_fk
        foreign key (changed_by_technician_id) references technicians(id),
    add constraint repair_request_status_history_changed_by_one_actor_check check (
        (changed_by_user_id is null and changed_by_technician_id is null)
        or (changed_by_user_id is not null and changed_by_technician_id is null)
        or (changed_by_user_id is null and changed_by_technician_id is not null)
    );

create index idx_repair_request_status_history_changed_by_technician
    on repair_request_status_history(changed_by_technician_id);

alter table repair_attachments
    add column uploaded_by_technician_id bigint;

alter table repair_attachments
    add constraint repair_attachments_uploaded_by_technician_fk
        foreign key (uploaded_by_technician_id) references technicians(id);

alter table repair_attachments
    drop constraint repair_attachments_uploader_check;

alter table repair_attachments
    add constraint repair_attachments_uploader_check check (
        (uploaded_by_user_id is not null and uploaded_by_customer_id is null and uploaded_by_technician_id is null)
        or (uploaded_by_user_id is null and uploaded_by_customer_id is not null and uploaded_by_technician_id is null)
        or (uploaded_by_user_id is null and uploaded_by_customer_id is null and uploaded_by_technician_id is not null)
    );

create index idx_repair_attachments_uploaded_by_technician
    on repair_attachments(uploaded_by_technician_id);

create table telegram_user_contexts (
    id bigserial primary key,
    telegram_user_id bigint not null,
    telegram_chat_id bigint not null,
    active_mode varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint telegram_user_contexts_user_unique unique (telegram_user_id),
    constraint telegram_user_contexts_chat_unique unique (telegram_chat_id),
    constraint telegram_user_contexts_mode_check check (active_mode in ('CUSTOMER', 'TECHNICIAN')),
    constraint telegram_user_contexts_updated_check check (updated_at >= created_at)
);

create index idx_telegram_user_contexts_mode on telegram_user_contexts(active_mode);

create table telegram_technician_link_tokens (
    id bigserial primary key,
    token_hash varchar(64) not null,
    technician_id bigint not null,
    created_by_user_id bigint not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    revoked_at timestamp with time zone,
    used_by_telegram_user_id bigint,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint telegram_technician_link_tokens_token_unique unique (token_hash),
    constraint telegram_technician_link_tokens_technician_fk foreign key (technician_id) references technicians(id) on delete cascade,
    constraint telegram_technician_link_tokens_created_by_user_fk foreign key (created_by_user_id) references users(id) on delete cascade,
    constraint telegram_technician_link_tokens_hash_check check (token_hash ~ '^[a-f0-9]{64}$'),
    constraint telegram_technician_link_tokens_state_check check (used_at is null or revoked_at is null),
    constraint telegram_technician_link_tokens_updated_check check (updated_at >= created_at)
);

create index idx_telegram_technician_link_tokens_technician
    on telegram_technician_link_tokens(technician_id);
create unique index idx_telegram_technician_link_tokens_active_technician
    on telegram_technician_link_tokens(technician_id)
    where used_at is null and revoked_at is null;

create table telegram_technician_sessions (
    id bigserial primary key,
    telegram_user_id bigint not null,
    telegram_chat_id bigint not null,
    technician_id bigint,
    language varchar(8) not null,
    state varchar(40) not null,
    pending_token_hash varchar(64),
    selected_request_id bigint,
    draft_text varchar(4000),
    last_interaction_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint telegram_technician_sessions_user_unique unique (telegram_user_id),
    constraint telegram_technician_sessions_chat_unique unique (telegram_chat_id),
    constraint telegram_technician_sessions_technician_fk foreign key (technician_id) references technicians(id) on delete cascade,
    constraint telegram_technician_sessions_request_fk foreign key (selected_request_id) references repair_requests(id),
    constraint telegram_technician_sessions_language_check check (language in ('EN', 'RU', 'UZ')),
    constraint telegram_technician_sessions_state_check check (state in (
        'LANGUAGE_SELECTION',
        'MAIN_MENU',
        'AWAITING_REJECTION_REASON',
        'AWAITING_DIAGNOSIS',
        'AWAITING_WAIT_REASON',
        'AWAITING_RESUME_NOTE',
        'AWAITING_WORK_PERFORMED',
        'AWAITING_DIAGNOSIS_PHOTO',
        'AWAITING_COMPLETION_PHOTO'
    )),
    constraint telegram_technician_sessions_pending_token_hash_check check (
        pending_token_hash is null or pending_token_hash ~ '^[a-f0-9]{64}$'
    ),
    constraint telegram_technician_sessions_updated_check check (updated_at >= created_at)
);

create index idx_telegram_technician_sessions_technician
    on telegram_technician_sessions(technician_id);
create index idx_telegram_technician_sessions_last_interaction
    on telegram_technician_sessions(last_interaction_at);
