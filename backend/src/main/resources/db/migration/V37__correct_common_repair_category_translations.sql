-- Correct common category rows where localized columns were saved as English copies.

update repair_categories rc
set name_uz = 'Noutbuk va kompyuter ta''miri',
    name_ru = 'Ремонт ноутбуков и компьютеров',
    name_uz_normalized = 'noutbuk-va-kompyuter-tamiri',
    name_ru_normalized = 'ремонт-ноутбуков-и-компьютеров',
    description_uz = coalesce(description_uz, 'Noutbuk va kompyuterlarni ta''mirlash'),
    description_ru = coalesce(description_ru, 'Ремонт ноутбуков и компьютеров')
where lower(name_en) in ('laptop and computer repair', 'laptop & computer repair', 'computer and laptop repair')
  and lower(name_uz) in ('laptop and computer repair', 'laptop & computer repair', 'computer and laptop repair')
  and lower(name_ru) in ('laptop and computer repair', 'laptop & computer repair', 'computer and laptop repair')
  and not exists (
      select 1
      from repair_categories existing
      where existing.id <> rc.id
        and (
            existing.name_uz_normalized = 'noutbuk-va-kompyuter-tamiri'
            or existing.name_ru_normalized = 'ремонт-ноутбуков-и-компьютеров'
        )
  );

update repair_categories rc
set name_uz = 'Mobil telefon ta''miri',
    name_ru = 'Ремонт мобильных телефонов',
    name_uz_normalized = 'mobil-telefon-tamiri',
    name_ru_normalized = 'ремонт-мобильных-телефонов',
    description_uz = coalesce(description_uz, 'Mobil telefonlarni ta''mirlash'),
    description_ru = coalesce(description_ru, 'Ремонт мобильных телефонов')
where lower(name_en) in ('mobile phone repair', 'phone repair', 'smartphone repair')
  and lower(name_uz) in ('mobile phone repair', 'phone repair', 'smartphone repair')
  and lower(name_ru) in ('mobile phone repair', 'phone repair', 'smartphone repair')
  and not exists (
      select 1
      from repair_categories existing
      where existing.id <> rc.id
        and (
            existing.name_uz_normalized = 'mobil-telefon-tamiri'
            or existing.name_ru_normalized = 'ремонт-мобильных-телефонов'
        )
  );
