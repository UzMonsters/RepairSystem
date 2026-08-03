create table repair_assignments (
    id bigserial primary key,
    repair_request_id bigint not null,
    technician_id bigint not null,
    status varchar(32) not null,
    scheduled_visit_at timestamp with time zone,
    assigned_by_user_id bigint not null,
    assigned_at timestamp with time zone not null,
    responded_at timestamp with time zone,
    rejection_reason varchar(500),
    closure_reason varchar(500),
    closed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint repair_assignments_request_fk foreign key (repair_request_id) references repair_requests(id) on delete cascade,
    constraint repair_assignments_technician_fk foreign key (technician_id) references technicians(id),
    constraint repair_assignments_assigned_by_user_fk foreign key (assigned_by_user_id) references users(id),
    constraint repair_assignments_status_check check (
        status in ('PENDING', 'ACCEPTED', 'REJECTED', 'UNASSIGNED', 'REASSIGNED')
    ),
    constraint repair_assignments_rejection_reason_not_blank_check check (
        rejection_reason is null or length(trim(rejection_reason)) > 0
    ),
    constraint repair_assignments_closure_reason_not_blank_check check (
        closure_reason is null or length(trim(closure_reason)) > 0
    ),
    constraint repair_assignments_closed_status_check check (
        (status in ('PENDING', 'ACCEPTED') and closed_at is null)
        or (status in ('REJECTED', 'UNASSIGNED', 'REASSIGNED') and closed_at is not null)
    ),
    constraint repair_assignments_responded_status_check check (
        (status in ('ACCEPTED', 'REJECTED') and responded_at is not null)
        or (status not in ('ACCEPTED', 'REJECTED'))
    ),
    constraint repair_assignments_timestamps_check check (updated_at >= created_at)
);

create index idx_repair_assignments_request_id on repair_assignments(repair_request_id);
create index idx_repair_assignments_technician_id on repair_assignments(technician_id);
create index idx_repair_assignments_status on repair_assignments(status);
create index idx_repair_assignments_scheduled_visit_at on repair_assignments(scheduled_visit_at);
create index idx_repair_assignments_technician_active_workload
    on repair_assignments(technician_id, status)
    where status in ('PENDING', 'ACCEPTED');

create unique index idx_repair_assignments_one_active_per_request
    on repair_assignments(repair_request_id)
    where status in ('PENDING', 'ACCEPTED');
