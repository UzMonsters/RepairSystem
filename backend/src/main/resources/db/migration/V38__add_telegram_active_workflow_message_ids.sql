alter table telegram_customer_sessions
    add column active_workflow_message_id bigint;

alter table telegram_technician_sessions
    add column active_workflow_message_id bigint;
