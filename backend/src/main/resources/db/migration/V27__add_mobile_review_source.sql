alter table repair_reviews
    drop constraint if exists repair_reviews_source_check;

alter table repair_reviews
    add constraint repair_reviews_source_check check (source in ('TELEGRAM', 'MOBILE'));
