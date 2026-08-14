create table user_settings (
    id bigserial primary key,
    user_id bigint not null unique references users(id) on delete cascade,
    language varchar(10) not null default 'UZ',
    date_format varchar(30) not null default 'DD_MM_YYYY',
    time_format varchar(20) not null default 'HOUR_24',
    theme varchar(20) not null default 'SYSTEM',
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table system_settings (
    id bigserial primary key,
    timezone varchar(64) not null default 'Asia/Tashkent',
    default_language varchar(10) not null default 'UZ',
    updated_at timestamp with time zone not null,
    updated_by bigint references users(id)
);

insert into system_settings (id, timezone, default_language, updated_at)
values (1, 'Asia/Tashkent', 'UZ', CURRENT_TIMESTAMP)
on conflict (id) do nothing;
