alter table customers
    drop constraint customers_preferred_language_check;

alter table customers
    add constraint customers_preferred_language_check check (preferred_language in ('EN', 'RU', 'UZ'));

alter table technicians
    add column preferred_language varchar(8) not null default 'UZ';

alter table technicians
    add constraint technicians_preferred_language_check check (preferred_language in ('EN', 'RU', 'UZ'));

alter table repair_categories
    add column name_en varchar(120),
    add column name_en_normalized varchar(120),
    add column description_en varchar(500);

update repair_categories
set name_en = name_uz,
    name_en_normalized = name_uz_normalized,
    description_en = description_uz
where name_en is null;

alter table repair_categories
    alter column name_en set not null,
    alter column name_en_normalized set not null;

alter table repair_categories
    add constraint repair_categories_name_en_not_blank_check check (length(trim(name_en)) > 0),
    add constraint repair_categories_name_en_normalized_not_blank_check check (length(trim(name_en_normalized)) > 0),
    add constraint repair_categories_description_en_not_blank_check check (
        description_en is null or length(trim(description_en)) > 0
    );

alter table repair_categories
    add constraint repair_categories_name_en_normalized_unique unique (name_en_normalized);
