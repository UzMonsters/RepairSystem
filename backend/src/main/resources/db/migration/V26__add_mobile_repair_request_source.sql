alter table repair_requests
    drop constraint if exists repair_requests_source_check;

alter table repair_requests
    add constraint repair_requests_source_check check (source in ('ADMIN', 'TELEGRAM', 'MOBILE'));

alter table repair_requests
    drop constraint if exists repair_requests_source_attribution_check;

alter table repair_requests
    add constraint repair_requests_source_attribution_check check (
        (source = 'ADMIN' and created_by_user_id is not null and source_reference is null)
        or (source = 'TELEGRAM' and created_by_user_id is null and source_reference is not null)
        or (source = 'MOBILE' and created_by_user_id is null and source_reference is not null)
    );
