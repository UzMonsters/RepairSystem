alter table notification_outbox drop constraint if exists notification_outbox_type_check;

alter table notification_outbox
    add constraint notification_outbox_type_check check (notification_type in (
        'REQUEST_CREATED',
        'REPAIR_STARTED',
        'REPAIR_COMPLETED',
        'REQUEST_CANCELLED',
        'TECHNICIAN_ASSIGNED',
        'TECHNICIAN_UNASSIGNED',
        'TECHNICIAN_REJECTED',
        'VISIT_SCHEDULED',
        'VISIT_RESCHEDULED',
        'VISIT_CANCELLED',
        'WAITING_FOR_PARTS',
        'REPAIR_RESUMED'
    ));
