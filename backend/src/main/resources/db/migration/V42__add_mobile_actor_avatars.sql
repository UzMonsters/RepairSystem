alter table customers
    add column avatar_attachment_id bigint;

alter table technicians
    add column avatar_attachment_id bigint;

alter table customers
    add constraint customers_avatar_attachment_fk
        foreign key (avatar_attachment_id) references repair_attachments(id);

alter table technicians
    add constraint technicians_avatar_attachment_fk
        foreign key (avatar_attachment_id) references repair_attachments(id);

create index idx_customers_avatar_attachment_id on customers(avatar_attachment_id);
create index idx_technicians_avatar_attachment_id on technicians(avatar_attachment_id);
