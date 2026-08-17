alter table notification_outbox drop constraint if exists notification_outbox_type_check;
alter table notification_outbox drop constraint if exists notification_outbox_channel_check;

alter table notification_outbox
    add column if not exists language varchar(10),
    add column if not exists rendered_title varchar(240),
    add column if not exists rendered_message varchar(4096);

update notification_outbox
set notification_type = case notification_type
    when 'CUSTOMER_REQUEST_CREATED' then 'REQUEST_CREATED'
    when 'CUSTOMER_REPAIR_COMPLETED' then 'REPAIR_COMPLETED'
    when 'CUSTOMER_REPAIR_STARTED' then 'REPAIR_STARTED'
    when 'CUSTOMER_VISIT_SCHEDULED' then 'VISIT_SCHEDULED'
    when 'TECHNICIAN_VISIT_SCHEDULED' then 'VISIT_SCHEDULED'
    when 'CUSTOMER_VISIT_RESCHEDULED' then 'VISIT_RESCHEDULED'
    when 'TECHNICIAN_VISIT_RESCHEDULED' then 'VISIT_RESCHEDULED'
    when 'CUSTOMER_VISIT_SCHEDULE_CLEARED' then 'VISIT_CANCELLED'
    when 'TECHNICIAN_VISIT_SCHEDULE_CLEARED' then 'VISIT_CANCELLED'
    when 'CUSTOMER_TECHNICIAN_ASSIGNED' then 'TECHNICIAN_ASSIGNED'
    when 'CUSTOMER_TECHNICIAN_REASSIGNED' then 'TECHNICIAN_ASSIGNED'
    when 'TECHNICIAN_NEW_ASSIGNMENT' then 'TECHNICIAN_ASSIGNED'
    when 'TECHNICIAN_REASSIGNED_TO_REQUEST' then 'TECHNICIAN_ASSIGNED'
    when 'CUSTOMER_TECHNICIAN_UNASSIGNED' then 'TECHNICIAN_UNASSIGNED'
    when 'TECHNICIAN_REMOVED_FROM_REQUEST' then 'TECHNICIAN_UNASSIGNED'
    when 'CUSTOMER_REQUEST_CANCELLED' then 'REQUEST_CANCELLED'
    when 'TECHNICIAN_REQUEST_CANCELLED' then 'REQUEST_CANCELLED'
    when 'CUSTOMER_WAITING_FOR_PARTS' then 'WAITING_FOR_PARTS'
    when 'CUSTOMER_REPAIR_RESUMED' then 'REPAIR_RESUMED'
    else notification_type
end
where notification_type in (
    'CUSTOMER_REQUEST_CREATED',
    'CUSTOMER_REPAIR_COMPLETED',
    'CUSTOMER_REPAIR_STARTED',
    'CUSTOMER_VISIT_SCHEDULED',
    'TECHNICIAN_VISIT_SCHEDULED',
    'CUSTOMER_VISIT_RESCHEDULED',
    'TECHNICIAN_VISIT_RESCHEDULED',
    'CUSTOMER_VISIT_SCHEDULE_CLEARED',
    'TECHNICIAN_VISIT_SCHEDULE_CLEARED',
    'CUSTOMER_TECHNICIAN_ASSIGNED',
    'CUSTOMER_TECHNICIAN_REASSIGNED',
    'TECHNICIAN_NEW_ASSIGNMENT',
    'TECHNICIAN_REASSIGNED_TO_REQUEST',
    'CUSTOMER_TECHNICIAN_UNASSIGNED',
    'TECHNICIAN_REMOVED_FROM_REQUEST',
    'CUSTOMER_REQUEST_CANCELLED',
    'TECHNICIAN_REQUEST_CANCELLED',
    'CUSTOMER_WAITING_FOR_PARTS',
    'CUSTOMER_REPAIR_RESUMED'
);

update notification_outbox
set template_key = 'notification.' || lower(replace(notification_type, '_', '.'))
where template_key is null
   or template_key like 'notification.customer.%'
   or template_key like 'notification.technician.%'
   or template_key like 'notification.customer_%'
   or template_key like 'notification.technician_%';

update notification_outbox
set language = coalesce(language, 'UZ'),
    rendered_title = coalesce(rendered_title, notification_type),
    rendered_message = coalesce(rendered_message, payload_json);

alter table notification_outbox
    alter column language set not null,
    alter column rendered_title set not null,
    alter column rendered_message set not null;

alter table notification_outbox
    add constraint notification_outbox_type_check check (notification_type in (
        'REQUEST_CREATED',
        'REPAIR_STARTED',
        'REPAIR_COMPLETED',
        'REQUEST_CANCELLED',
        'TECHNICIAN_ASSIGNED',
        'TECHNICIAN_UNASSIGNED',
        'VISIT_SCHEDULED',
        'VISIT_RESCHEDULED',
        'VISIT_CANCELLED',
        'WAITING_FOR_PARTS',
        'REPAIR_RESUMED'
    ));

alter table notification_outbox
    add constraint notification_outbox_channel_check check (channel in ('TELEGRAM', 'EMAIL', 'PUSH'));
