alter table repair_requests
    add column deleted_by_user_id bigint,
    add column deleted_at timestamp with time zone;

alter table repair_requests
    add constraint fk_repair_requests_deleted_by_user
        foreign key (deleted_by_user_id) references users (id);

create index idx_repair_requests_visible_created
    on repair_requests (created_at desc, id desc)
    where deleted_at is null;

create index idx_repair_requests_deleted_by_user
    on repair_requests (deleted_by_user_id)
    where deleted_by_user_id is not null;
