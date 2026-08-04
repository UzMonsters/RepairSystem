create table repair_attachments (
    id bigserial primary key,
    repair_request_id bigint not null,
    attachment_type varchar(40) not null,
    status varchar(20) not null,
    storage_key varchar(255) not null,
    original_file_name varchar(255) not null,
    content_type varchar(80),
    size_bytes bigint,
    sha256_checksum varchar(64),
    uploaded_by_user_id bigint not null,
    uploaded_at timestamp with time zone not null,
    available_at timestamp with time zone,
    deleted_by_user_id bigint,
    deleted_at timestamp with time zone,
    deletion_reason varchar(1000),
    failure_reason varchar(120),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint repair_attachments_request_fk foreign key (repair_request_id) references repair_requests(id),
    constraint repair_attachments_uploaded_by_user_fk foreign key (uploaded_by_user_id) references users(id),
    constraint repair_attachments_deleted_by_user_fk foreign key (deleted_by_user_id) references users(id),
    constraint repair_attachments_type_check check (
        attachment_type in ('CUSTOMER_PROBLEM_PHOTO', 'DIAGNOSIS_PHOTO', 'COMPLETION_PHOTO', 'GENERAL_DOCUMENT')
    ),
    constraint repair_attachments_status_check check (
        status in ('UPLOADING', 'AVAILABLE', 'FAILED', 'DELETED')
    ),
    constraint repair_attachments_storage_key_unique unique (storage_key),
    constraint repair_attachments_size_non_negative_check check (size_bytes is null or size_bytes >= 0),
    constraint repair_attachments_checksum_check check (
        sha256_checksum is null or sha256_checksum ~ '^[a-f0-9]{64}$'
    ),
    constraint repair_attachments_available_check check (
        (status = 'AVAILABLE' and available_at is not null and content_type is not null
            and size_bytes is not null and sha256_checksum is not null)
        or status <> 'AVAILABLE'
    ),
    constraint repair_attachments_deleted_check check (
        (status = 'DELETED' and deleted_at is not null and deleted_by_user_id is not null)
        or status <> 'DELETED'
    ),
    constraint repair_attachments_failure_check check (
        (status = 'FAILED' and failure_reason is not null)
        or status <> 'FAILED'
    ),
    constraint repair_attachments_updated_check check (updated_at >= created_at)
);

create index idx_repair_attachments_request_id on repair_attachments(repair_request_id);
create index idx_repair_attachments_request_type on repair_attachments(repair_request_id, attachment_type);
create index idx_repair_attachments_status on repair_attachments(status);
create index idx_repair_attachments_uploaded_at on repair_attachments(uploaded_at);
create index idx_repair_attachments_uploaded_by on repair_attachments(uploaded_by_user_id);
create index idx_repair_attachments_available
    on repair_attachments(repair_request_id, attachment_type, uploaded_at desc)
    where status = 'AVAILABLE';
