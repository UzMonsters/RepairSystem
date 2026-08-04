create sequence repair_request_number_seq
    as bigint
    start with 1
    increment by 1
    minvalue 1
    no cycle;

create table repair_requests (
    id bigserial primary key,
    request_number varchar(32) not null,
    customer_id bigint not null,
    category_id bigint not null,
    description varchar(2000) not null,
    address varchar(500),
    latitude numeric(9,6),
    longitude numeric(10,6),
    priority varchar(16) not null,
    status varchar(32) not null,
    source varchar(32) not null,
    customer_preferred_visit_at timestamp with time zone,
    internal_note varchar(2000),
    created_by_user_id bigint not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint repair_requests_request_number_unique unique (request_number),
    constraint repair_requests_customer_fk foreign key (customer_id) references customers(id),
    constraint repair_requests_category_fk foreign key (category_id) references repair_categories(id),
    constraint repair_requests_created_by_user_fk foreign key (created_by_user_id) references users(id),
    constraint repair_requests_description_length_check check (length(trim(description)) between 10 and 2000),
    constraint repair_requests_address_not_blank_check check (address is null or length(trim(address)) > 0),
    constraint repair_requests_internal_note_not_blank_check check (internal_note is null or length(trim(internal_note)) > 0),
    constraint repair_requests_priority_check check (priority in ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    constraint repair_requests_status_check check (
        status in ('NEW', 'ASSIGNED', 'SCHEDULED', 'IN_PROGRESS', 'WAITING_FOR_PARTS', 'COMPLETED', 'CANCELLED')
    ),
    constraint repair_requests_source_check check (source in ('ADMIN', 'TELEGRAM')),
    constraint repair_requests_latitude_check check (latitude is null or latitude between -90 and 90),
    constraint repair_requests_longitude_check check (longitude is null or longitude between -180 and 180),
    constraint repair_requests_coordinate_pair_check check (
        (latitude is null and longitude is null)
        or (latitude is not null and longitude is not null)
    ),
    constraint repair_requests_location_present_check check (
        (address is not null and length(trim(address)) > 0)
        or (latitude is not null and longitude is not null)
    ),
    constraint repair_requests_created_updated_check check (updated_at >= created_at)
);

create index idx_repair_requests_customer_id on repair_requests(customer_id);
create index idx_repair_requests_category_id on repair_requests(category_id);
create index idx_repair_requests_status on repair_requests(status);
create index idx_repair_requests_priority on repair_requests(priority);
create index idx_repair_requests_source on repair_requests(source);
create index idx_repair_requests_created_at on repair_requests(created_at);
create index idx_repair_requests_customer_preferred_visit_at on repair_requests(customer_preferred_visit_at);
