create table customers (
    id bigserial primary key,
    full_name varchar(160) not null,
    phone varchar(13) not null,
    telegram_user_id bigint,
    telegram_chat_id bigint,
    preferred_language varchar(8) not null,
    registration_source varchar(32) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint customers_full_name_not_blank_check check (length(trim(full_name)) > 0),
    constraint customers_phone_format_check check (phone ~ '^\+998[0-9]{9}$'),
    constraint customers_preferred_language_check check (preferred_language in ('UZ', 'RU')),
    constraint customers_registration_source_check check (registration_source in ('ADMIN', 'TELEGRAM')),
    constraint customers_created_updated_check check (updated_at >= created_at)
);

alter table customers
    add constraint customers_phone_unique unique (phone);

create unique index customers_telegram_user_id_unique
    on customers(telegram_user_id)
    where telegram_user_id is not null;

create index idx_customers_active on customers(active);
create index idx_customers_preferred_language on customers(preferred_language);
create index idx_customers_registration_source on customers(registration_source);
create index idx_customers_created_at on customers(created_at);
create index idx_customers_full_name_lower on customers(lower(full_name));

create table technicians (
    id bigserial primary key,
    full_name varchar(160) not null,
    phone varchar(13) not null,
    specialization varchar(120),
    notes varchar(1000),
    maximum_concurrent_requests integer not null default 5,
    active boolean not null default true,
    telegram_user_id bigint,
    telegram_chat_id bigint,
    telegram_linked_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint technicians_full_name_not_blank_check check (length(trim(full_name)) > 0),
    constraint technicians_phone_format_check check (phone ~ '^\+998[0-9]{9}$'),
    constraint technicians_maximum_concurrent_requests_check check (maximum_concurrent_requests > 0),
    constraint technicians_specialization_not_blank_check check (
        specialization is null or length(trim(specialization)) > 0
    ),
    constraint technicians_notes_not_blank_check check (notes is null or length(trim(notes)) > 0),
    constraint technicians_created_updated_check check (updated_at >= created_at)
);

alter table technicians
    add constraint technicians_phone_unique unique (phone);

create unique index technicians_telegram_user_id_unique
    on technicians(telegram_user_id)
    where telegram_user_id is not null;

create index idx_technicians_active on technicians(active);
create index idx_technicians_specialization_lower on technicians(lower(specialization));
create index idx_technicians_created_at on technicians(created_at);

create table repair_categories (
    id bigserial primary key,
    name_uz varchar(120) not null,
    name_ru varchar(120) not null,
    name_uz_normalized varchar(120) not null,
    name_ru_normalized varchar(120) not null,
    description_uz varchar(500),
    description_ru varchar(500),
    active boolean not null default true,
    display_order integer not null default 0,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint repair_categories_name_uz_not_blank_check check (length(trim(name_uz)) > 0),
    constraint repair_categories_name_ru_not_blank_check check (length(trim(name_ru)) > 0),
    constraint repair_categories_name_uz_normalized_not_blank_check check (length(trim(name_uz_normalized)) > 0),
    constraint repair_categories_name_ru_normalized_not_blank_check check (length(trim(name_ru_normalized)) > 0),
    constraint repair_categories_display_order_check check (display_order >= 0),
    constraint repair_categories_description_uz_not_blank_check check (
        description_uz is null or length(trim(description_uz)) > 0
    ),
    constraint repair_categories_description_ru_not_blank_check check (
        description_ru is null or length(trim(description_ru)) > 0
    ),
    constraint repair_categories_created_updated_check check (updated_at >= created_at)
);

alter table repair_categories
    add constraint repair_categories_name_uz_normalized_unique unique (name_uz_normalized);

alter table repair_categories
    add constraint repair_categories_name_ru_normalized_unique unique (name_ru_normalized);

create index idx_repair_categories_active on repair_categories(active);
create index idx_repair_categories_display_order on repair_categories(display_order);
create index idx_repair_categories_name_uz_lower on repair_categories(lower(name_uz));
create index idx_repair_categories_name_ru_lower on repair_categories(lower(name_ru));
