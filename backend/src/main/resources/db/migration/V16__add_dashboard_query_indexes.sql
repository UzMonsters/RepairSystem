create index idx_repair_executions_completed_at
    on repair_executions(completed_at)
    where completed_at is not null;

create index idx_repair_executions_cancelled_at
    on repair_executions(cancelled_at)
    where cancelled_at is not null;

create index idx_repair_requests_created_category
    on repair_requests(created_at, category_id);
