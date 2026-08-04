alter table users
    add column auth_version bigint;

update users
set auth_version = 1
where auth_version is null;

alter table users
    alter column auth_version set not null,
    alter column auth_version set default 1;

alter table users
    add constraint users_auth_version_positive_check check (auth_version > 0);
