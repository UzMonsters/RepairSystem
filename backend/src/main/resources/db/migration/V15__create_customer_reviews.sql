create table repair_reviews (
    id bigserial primary key,
    repair_request_id bigint not null,
    customer_id bigint not null,
    technician_id bigint not null,
    rating integer not null,
    comment varchar(1000),
    source varchar(20) not null,
    submitted_language varchar(8) not null,
    submitted_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint repair_reviews_request_unique unique (repair_request_id),
    constraint repair_reviews_request_fk foreign key (repair_request_id) references repair_requests(id),
    constraint repair_reviews_customer_fk foreign key (customer_id) references customers(id),
    constraint repair_reviews_technician_fk foreign key (technician_id) references technicians(id),
    constraint repair_reviews_rating_check check (rating between 1 and 5),
    constraint repair_reviews_comment_check check (
        comment is null or length(trim(comment)) between 1 and 1000
    ),
    constraint repair_reviews_source_check check (source in ('TELEGRAM')),
    constraint repair_reviews_language_check check (submitted_language in ('EN', 'RU', 'UZ')),
    constraint repair_reviews_created_check check (created_at <= submitted_at)
);

create index idx_repair_reviews_customer_id on repair_reviews(customer_id);
create index idx_repair_reviews_technician_id on repair_reviews(technician_id);
create index idx_repair_reviews_rating on repair_reviews(rating);
create index idx_repair_reviews_submitted_at on repair_reviews(submitted_at);

alter table telegram_customer_sessions
    add column review_request_id bigint;

alter table telegram_customer_sessions
    add column draft_review_rating integer;

alter table telegram_customer_sessions
    add column draft_review_comment varchar(1000);

alter table telegram_customer_sessions
    add constraint telegram_customer_sessions_review_request_fk
        foreign key (review_request_id) references repair_requests(id);

alter table telegram_customer_sessions
    add constraint telegram_customer_sessions_review_rating_check
        check (draft_review_rating is null or draft_review_rating between 1 and 5);

alter table telegram_customer_sessions
    add constraint telegram_customer_sessions_review_comment_check
        check (
            draft_review_comment is null
            or length(trim(draft_review_comment)) between 1 and 1000
        );

alter table telegram_customer_sessions
    drop constraint telegram_customer_sessions_state_check;

alter table telegram_customer_sessions
    add constraint telegram_customer_sessions_state_check check (state in (
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
        'UPDATING_PROFILE_PHONE',
        'SELECTING_REVIEW_REQUEST',
        'SELECTING_REVIEW_RATING',
        'AWAITING_REVIEW_COMMENT',
        'CONFIRMING_REVIEW'
    ));

create index idx_telegram_customer_sessions_review_request
    on telegram_customer_sessions(review_request_id);
