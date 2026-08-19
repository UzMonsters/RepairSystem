alter table push_endpoints
    rename column firebase_installation_id to fcm_registration_token;

alter table push_endpoints
    drop constraint if exists push_endpoints_installation_not_blank_check;

alter table push_endpoints
    add constraint push_endpoints_fcm_registration_token_not_blank_check
        check (length(trim(fcm_registration_token)) > 0);

alter table push_endpoints
    drop constraint if exists push_endpoints_installation_unique;

alter table push_endpoints
    add constraint push_endpoints_fcm_registration_token_unique
        unique (firebase_app_key, fcm_registration_token);
