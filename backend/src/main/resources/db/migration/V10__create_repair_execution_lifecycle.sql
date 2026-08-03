alter table repair_assignments
    drop constraint repair_assignments_status_check;

alter table repair_assignments
    drop constraint repair_assignments_closed_status_check;

alter table repair_assignments
    add constraint repair_assignments_status_check check (
        status in ('PENDING', 'ACCEPTED', 'REJECTED', 'UNASSIGNED', 'REASSIGNED', 'COMPLETED', 'CANCELLED')
    );

alter table repair_assignments
    add constraint repair_assignments_closed_status_check check (
        (status in ('PENDING', 'ACCEPTED') and closed_at is null)
        or (status in ('REJECTED', 'UNASSIGNED', 'REASSIGNED', 'COMPLETED', 'CANCELLED') and closed_at is not null)
    );

create table repair_executions (
    id bigserial primary key,
    repair_request_id bigint not null,
    started_at timestamp with time zone,
    started_by_user_id bigint,
    diagnosis varchar(4000),
    diagnosis_updated_at timestamp with time zone,
    diagnosis_updated_by_user_id bigint,
    waiting_reason varchar(1000),
    waiting_since timestamp with time zone,
    work_performed varchar(4000),
    completion_note varchar(2000),
    completed_at timestamp with time zone,
    completed_by_user_id bigint,
    cancellation_reason varchar(1000),
    cancelled_at timestamp with time zone,
    cancelled_by_user_id bigint,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint repair_executions_request_unique unique (repair_request_id),
    constraint repair_executions_request_fk foreign key (repair_request_id) references repair_requests(id),
    constraint repair_executions_started_by_user_fk foreign key (started_by_user_id) references users(id),
    constraint repair_executions_diagnosis_updated_by_user_fk foreign key (diagnosis_updated_by_user_id) references users(id),
    constraint repair_executions_completed_by_user_fk foreign key (completed_by_user_id) references users(id),
    constraint repair_executions_cancelled_by_user_fk foreign key (cancelled_by_user_id) references users(id),
    constraint repair_executions_started_user_pair_check check (
        (started_at is null and started_by_user_id is null)
        or (started_at is not null and started_by_user_id is not null)
    ),
    constraint repair_executions_diagnosis_not_blank_check check (
        diagnosis is null or length(trim(diagnosis)) > 0
    ),
    constraint repair_executions_diagnosis_update_pair_check check (
        (diagnosis is null and diagnosis_updated_at is null and diagnosis_updated_by_user_id is null)
        or (diagnosis is not null and diagnosis_updated_at is not null and diagnosis_updated_by_user_id is not null)
    ),
    constraint repair_executions_waiting_reason_not_blank_check check (
        waiting_reason is null or length(trim(waiting_reason)) > 0
    ),
    constraint repair_executions_waiting_pair_check check (
        (waiting_reason is null and waiting_since is null)
        or (waiting_reason is not null and waiting_since is not null)
    ),
    constraint repair_executions_work_performed_not_blank_check check (
        work_performed is null or length(trim(work_performed)) > 0
    ),
    constraint repair_executions_completion_note_not_blank_check check (
        completion_note is null or length(trim(completion_note)) > 0
    ),
    constraint repair_executions_completion_pair_check check (
        (completed_at is null and completed_by_user_id is null)
        or (completed_at is not null and completed_by_user_id is not null and work_performed is not null)
    ),
    constraint repair_executions_cancellation_reason_not_blank_check check (
        cancellation_reason is null or length(trim(cancellation_reason)) > 0
    ),
    constraint repair_executions_cancellation_pair_check check (
        (cancelled_at is null and cancelled_by_user_id is null and cancellation_reason is null)
        or (cancelled_at is not null and cancelled_by_user_id is not null and cancellation_reason is not null)
    ),
    constraint repair_executions_terminal_exclusive_check check (
        completed_at is null or cancelled_at is null
    ),
    constraint repair_executions_updated_check check (updated_at >= created_at)
);

create table repair_request_status_history (
    id bigserial primary key,
    repair_request_id bigint not null,
    from_status varchar(32),
    to_status varchar(32) not null,
    reason varchar(1000),
    changed_by_user_id bigint,
    changed_at timestamp with time zone not null,
    constraint repair_request_status_history_request_fk foreign key (repair_request_id) references repair_requests(id),
    constraint repair_request_status_history_changed_by_user_fk foreign key (changed_by_user_id) references users(id),
    constraint repair_request_status_history_from_status_check check (
        from_status is null or from_status in (
            'NEW', 'ASSIGNED', 'SCHEDULED', 'IN_PROGRESS', 'WAITING_FOR_PARTS', 'COMPLETED', 'CANCELLED'
        )
    ),
    constraint repair_request_status_history_to_status_check check (
        to_status in ('NEW', 'ASSIGNED', 'SCHEDULED', 'IN_PROGRESS', 'WAITING_FOR_PARTS', 'COMPLETED', 'CANCELLED')
    ),
    constraint repair_request_status_history_reason_not_blank_check check (
        reason is null or length(trim(reason)) > 0
    )
);

create index idx_repair_executions_request_id on repair_executions(repair_request_id);
create index idx_repair_request_status_history_request_changed_at
    on repair_request_status_history(repair_request_id, changed_at desc, id desc);
create index idx_repair_request_status_history_to_status on repair_request_status_history(to_status);

insert into repair_request_status_history (
    repair_request_id,
    from_status,
    to_status,
    reason,
    changed_by_user_id,
    changed_at
)
select
    id,
    null,
    status,
    'Phase 5 backfill of current request status.',
    created_by_user_id,
    created_at
from repair_requests;
