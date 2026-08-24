-- V40: Enforce strict mobile sessions invariant
-- Clean up any legacy mobile refresh sessions that have no associated mobile_session_id
delete from mobile_refresh_sessions
where mobile_session_id is null;

-- Make mobile_session_id mandatory on mobile_refresh_sessions
alter table mobile_refresh_sessions
    alter column mobile_session_id set not null;

-- Ensure constraint is present
alter table mobile_refresh_sessions
    drop constraint if exists mobile_refresh_sessions_session_id_check;

alter table mobile_refresh_sessions
    add constraint mobile_refresh_sessions_session_id_check
    check (mobile_session_id is not null);
