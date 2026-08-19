alter table repair_requests
    rename column latitude to location_latitude;

alter table repair_requests
    rename column longitude to location_longitude;

alter table repair_requests
    rename column address to location_address;

alter table repair_requests
    alter column location_latitude type numeric(10,7),
    alter column location_longitude type numeric(10,7);

alter table repair_requests
    add column location_source varchar(30);

alter table repair_requests
    drop constraint if exists repair_requests_location_present_check,
    drop constraint if exists repair_requests_latitude_check,
    drop constraint if exists repair_requests_longitude_check,
    drop constraint if exists repair_requests_coordinate_pair_check,
    drop constraint if exists repair_requests_address_not_blank_check;

alter table repair_requests
    add constraint repair_requests_location_latitude_check
        check (location_latitude is null or location_latitude between -90 and 90),
    add constraint repair_requests_location_longitude_check
        check (location_longitude is null or location_longitude between -180 and 180),
    add constraint repair_requests_location_coordinate_pair_check
        check (
            (location_latitude is null and location_longitude is null)
            or (location_latitude is not null and location_longitude is not null)
        ),
    add constraint repair_requests_location_address_not_blank_check
        check (location_address is null or length(trim(location_address)) > 0),
    add constraint repair_requests_location_source_check
        check (location_source is null or location_source in ('TELEGRAM', 'DEVICE_GPS', 'MAP_PIN', 'MANUAL'));

alter table telegram_customer_sessions
    drop constraint if exists telegram_customer_sessions_state_check;

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
        'AWAITING_LOCATION_ADDRESS',
        'CONFIRMING_REQUEST',
        'UPDATING_PROFILE_NAME',
        'UPDATING_PROFILE_PHONE',
        'SELECTING_REVIEW_REQUEST',
        'SELECTING_REVIEW_RATING',
        'AWAITING_REVIEW_COMMENT',
        'CONFIRMING_REVIEW'
    ));
