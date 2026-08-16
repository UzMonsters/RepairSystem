-- Migration V22: Correct historical Repair Category English translations
-- Fix categories where V8 copied Uzbek names/descriptions directly into English columns

update repair_categories
set name_en = 'Air Conditioner',
    name_en_normalized = 'air-conditioner',
    description_en = coalesce(description_en, 'Air conditioning repair and maintenance')
where lower(name_uz) in ('konditsioner', 'konditsioner ta''mirlash')
  and (name_en = name_uz or lower(name_en) in ('konditsioner', 'konditsioner ta''mirlash'));

update repair_categories
set name_en = 'Refrigerator',
    name_en_normalized = 'refrigerator',
    description_en = coalesce(description_en, 'Refrigerator repair and maintenance')
where lower(name_uz) in ('muzlatgich', 'holodilnik', 'muzlatgich ta''mirlash')
  and (name_en = name_uz or lower(name_en) in ('muzlatgich', 'holodilnik', 'muzlatgich ta''mirlash'));

update repair_categories
set name_en = 'Washing Machine',
    name_en_normalized = 'washing-machine',
    description_en = coalesce(description_en, 'Washing machine repair and maintenance')
where lower(name_uz) in ('kir yuvish mashinasi', 'stiralnaya mashina')
  and (name_en = name_uz or lower(name_en) in ('kir yuvish mashinasi', 'stiralnaya mashina'));
