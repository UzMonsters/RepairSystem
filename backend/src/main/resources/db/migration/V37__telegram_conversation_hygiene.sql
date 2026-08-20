alter table telegram_customer_sessions
    add column active_prompt_message_id bigint,
    add column transient_message_ids text;

alter table telegram_technician_sessions
    add column active_prompt_message_id bigint,
    add column transient_message_ids text;
