alter table users
    add constraint users_email_unique unique (email);

alter table refresh_sessions
    add constraint refresh_sessions_token_hash_unique unique (token_hash);

create index idx_users_role on users(role);
create index idx_users_active on users(active);
create index idx_users_full_name_lower on users(lower(full_name));

create index idx_refresh_sessions_user_id on refresh_sessions(user_id);
create index idx_refresh_sessions_token_family_id on refresh_sessions(token_family_id);
create index idx_refresh_sessions_expires_at on refresh_sessions(expires_at);
create index idx_refresh_sessions_revoked_at on refresh_sessions(revoked_at);
create index idx_refresh_sessions_user_active on refresh_sessions(user_id, revoked_at, expires_at);
