create table users (
    id bigserial primary key,
    full_name varchar(160) not null,
    email varchar(320) not null,
    password_hash varchar(255) not null,
    role varchar(32) not null,
    active boolean not null default true,
    password_changed_at timestamp with time zone not null,
    last_login_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    version bigint not null default 0,
    constraint users_role_check check (role in ('ADMIN', 'MANAGER')),
    constraint users_email_lowercase_check check (email = lower(email)),
    constraint users_email_not_blank_check check (length(trim(email)) > 0),
    constraint users_full_name_not_blank_check check (length(trim(full_name)) > 0),
    constraint users_password_hash_not_blank_check check (length(trim(password_hash)) > 0),
    constraint users_created_updated_check check (updated_at >= created_at)
);
