alter table telegram_updates
    drop constraint telegram_updates_status_check;

alter table telegram_updates
    add constraint telegram_updates_status_check
        check (status in ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED'));
