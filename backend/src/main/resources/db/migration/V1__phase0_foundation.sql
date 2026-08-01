create table phase0_schema_marker (
    id smallint primary key,
    description varchar(120) not null,
    created_at timestamp with time zone not null default now(),
    constraint phase0_schema_marker_singleton check (id = 1)
);

insert into phase0_schema_marker (id, description)
values (1, 'Phase 0 Flyway migration infrastructure marker');
