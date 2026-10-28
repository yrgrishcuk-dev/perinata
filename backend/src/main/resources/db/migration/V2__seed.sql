insert into clinic (name, timezone)
values ('КДЦ ПЦ', 'Europe/Moscow');

insert into content_release (version, approved_by, approved_at)
values ('demo-2026-10', 'demo', now());

insert into app_setting (setting_key, setting_value)
values ('content.version', 'demo-2026-10'),
       ('link.token.ttl.minutes', '15'),
       ('reminder.weekly.symptoms.day', 'MONDAY'),
       ('reminder.weekly.symptoms.time', '10:00');

insert into threshold_default (code, value, severity, version)
values ('SBP_WARN', 140, 'WARNING', 'clinic-2026-10'),
       ('SBP_CRIT', 160, 'CRITICAL', 'clinic-2026-10'),
       ('DBP_WARN', 90, 'WARNING', 'clinic-2026-10'),
       ('DBP_CRIT', 110, 'CRITICAL', 'clinic-2026-10'),
       ('GLU_FAST_WARN', 5.1, 'WARNING', 'clinic-2026-10'),
       ('GLU_FAST_CRIT', 7.0, 'CRITICAL', 'clinic-2026-10'),
       ('GLU_POST_WARN', 6.7, 'WARNING', 'clinic-2026-10'),
       ('GLU_POST_CRIT', 11.1, 'CRITICAL', 'clinic-2026-10'),
       ('WEIGHT_GAIN_WARN', 2.0, 'WARNING', 'clinic-2026-10');

insert into clinical_rule (code, version, params, active)
values ('CONTRACTION_PRETERM', 'clinic-2026-10',
        $json${"weeksBefore":37,"count":4,"windowMinutes":60,"message":"Признаки преждевременных родов. Срочно обратитесь в стационар или вызовите 103."}$json$,
        true),
       ('CONTRACTION_TERM_WARN', 'clinic-2026-10',
        $json${"weeksFrom":37,"count":4,"windowMinutes":60,"message":"Схватки чаще 4 раз в час. Сообщите врачу и продолжайте наблюдение."}$json$,
        true),
       ('CONTRACTION_TERM_GO', 'clinic-2026-10',
        $json${"weeksFrom":37,"intervalMinutes":5,"durationSeconds":60,"forMinutes":60,"count":10,"message":"Регулярные схватки каждые 5 минут в течение часа. Пора в родильный дом."}$json$,
        true);

insert into gain_corridor (bmi_category, week, min_gain_kg, max_gain_kg, version)
select c.cat,
       gs.week,
       case
           when gs.week <= 13 then 0.50
           else round(least(c.total_min, 0.50 + (gs.week - 13) * ((c.total_min - 0.50) / 27.0)), 2)
           end,
       case
           when gs.week <= 13 then 2.00
           else round(least(c.total_max, 2.00 + (gs.week - 13) * ((c.total_max - 2.00) / 27.0)), 2)
           end,
       'iom-2009'
from (values ('UNDERWEIGHT', 12.5, 18.0),
             ('NORMAL', 11.5, 16.0),
             ('OVERWEIGHT', 7.0, 11.5),
             ('OBESE', 5.0, 9.0)) as c(cat, total_min, total_max)
         cross join generate_series(1, 42) as gs(week);

insert into week_content (week_from, week_to, version, title, fetal_text, nutrition_text, warning_text)
select gs.week,
       gs.week,
       'demo-2026-10',
       'Неделя ' || gs.week,
       'Демонстрационный текст о развитии плода на ' || gs.week || '-й неделе. Замените утверждённым контентом клиники.',
       'На ' || gs.week || '-й неделе важно регулярное сбалансированное питание: белок, овощи, цельнозерновые продукты, достаточное количество воды. Точный рацион — в разделе «Питание».',
       'При кровянистых выделениях, обильных жидких выделениях, сильной боли, головной боли с нарушением зрения, внезапных отёках, температуре 38 °C и выше — немедленно обратитесь за медицинской помощью.'
from generate_series(4, 42) as gs(week);

update week_content
set fetal_text     = 'Идёт имплантация и закладка основных структур. Нервная трубка формируется, поэтому особенно важен фолат.',
    nutrition_text = 'Фолаты: тёмно-зелёные овощи, бобовые, печень — только по согласованию с врачом. Откажитесь от алкоголя. Питание дробное, без резких ограничений.',
    title          = 'Неделя 5: закладка основ'
where week_from = 5;

update week_content
set fetal_text     = 'Формируются сердце, конечности и лицо. Это период максимальной уязвимости к внешним воздействиям.',
    nutrition_text = 'Белок и фолаты каждый день. Избегайте сырых яиц, непастеризованных молочных продуктов и сырой рыбы.',
    title          = 'Неделя 8: органы формируются'
where week_from = 8;

update week_content
set fetal_text     = 'Плод активно растёт, сформированы основные органы. Впереди первый скрининг.',
    nutrition_text = 'Добавьте продукты, богатые кальцием и железом: молочные продукты, рыба, мясо, бобовые. Витамин C помогает усвоению железа.',
    title          = 'Неделя 12: конец первого триместра'
where week_from = 12;

update week_content
set fetal_text     = 'Скелет укрепляется, движения становятся более координированными. Вы можете начать чувствовать шевеления.',
    nutrition_text = 'Следите за прибавкой веса по коридору. Белок — 1–1,5 г на кг веса, если врач не назначил иначе. Ограничьте сладкие напитки и выпечку.',
    title          = 'Неделя 16: движения и рост'
where week_from = 16;

update week_content
set fetal_text     = 'Плод слышит звуки, активно двигается. На УЗИ оценивают органы и рост.',
    nutrition_text = 'Железо: мясо, гречка, бобовые. Кальций: молочные продукты, кунжут, зелень. Пейте воду, ограничьте кофеин до 200 мг в день.',
    title          = 'Неделя 20: середина беременности'
where week_from = 20;

update week_content
set fetal_text     = 'Плод набирает вес, лёгкие готовятся к дыханию. Врач может назначить тест на гестационный диабет.',
    nutrition_text = 'Если назначен тест на глюкозу — следуйте инструкции врача. В повседневном рационе меньше быстрых углеводов, больше овощей и белка.',
    title          = 'Неделя 24: подготовка к третьему триместру'
where week_from = 24;

update week_content
set fetal_text     = 'Начало третьего триместра. Плод активно набирает массу, шевеления хорошо ощутимы.',
    nutrition_text = 'Контролируйте прибавку веса. Дробное питание 5–6 раз в день помогает легче переносить нагрузку. Не ограничивайте соль без рекомендации врача.',
    title          = 'Неделя 28: третий триместр'
where week_from = 28;

update week_content
set fetal_text     = 'Плод растёт быстрее, кожа становится гладкой, подкожный жир накапливается.',
    nutrition_text = 'Белок и кальций важны для роста. Следите за отёками и давлением, измеряйте АД в одно и то же время.',
    title          = 'Неделя 32: активный рост'
where week_from = 32;

update week_content
set fetal_text     = 'Плод занимает положение для родов, живот опускается. Дыхание может стать легче.',
    nutrition_text = 'Не переедайте перед сном, выбирайте лёгкий ужин. Продолжайте пить воду и следить за прибавкой.',
    title          = 'Неделя 36: подготовка к встрече'
where week_from = 36;

update week_content
set fetal_text     = 'Плод готов к рождению. Врач оценит положение, вес и состояние плаценты.',
    nutrition_text = 'Питание остаётся регулярным и сбалансированным. Обсудите с врачом план на случай начала родов.',
    title          = 'Неделя 40: предполагаемая дата родов'
where week_from = 40;

insert into ration_plan (bmi_category, version, title, kcal, protein_g, fat_g, carbs_g, meals)
values ('UNDERWEIGHT', 'demo-2026-10', 'Рацион при дефиците массы', 2600, 110, 85, 330, $json$[
  {"meal":"Завтрак","items":"Овсяная каша на молоке, яйцо, яблоко, чай"},
  {"meal":"Второй завтрак","items":"Творог с ягодами, горсть орехов"},
  {"meal":"Обед","items":"Суп на курином бульоне, мясо или рыба, овощи, цельнозерновой хлеб"},
  {"meal":"Полдник","items":"Йогурт, банан"},
  {"meal":"Ужин","items":"Омлет с овощами, салат, картофель или крупа"}
]$json$),
       ('NORMAL', 'demo-2026-10', 'Рацион при нормальном ИМТ', 2300, 95, 75, 290, $json$[
         {"meal":"Завтрак","items":"Каша, яйцо или сыр, овощи, чай"},
         {"meal":"Второй завтрак","items":"Фрукт, йогурт"},
         {"meal":"Обед","items":"Суп, мясо или рыба, гарнир из овощей, хлеб"},
         {"meal":"Полдник","items":"Творог или орехи"},
         {"meal":"Ужин","items":"Рыба или курица, овощной салат, крупа"}
       ]$json$),
       ('OVERWEIGHT', 'demo-2026-10', 'Рацион при избыточной массе', 2000, 90, 65, 230, $json$[
         {"meal":"Завтрак","items":"Овсянка на воде или молоке, яйцо, овощи"},
         {"meal":"Второй завтрак","items":"Яблоко, кефир"},
         {"meal":"Обед","items":"Овощной суп, мясо или рыба, большая порция овощей"},
         {"meal":"Полдник","items":"Творог без сахара"},
         {"meal":"Ужин","items":"Белковый продукт, овощи, немного крупы"}
       ]$json$),
       ('OBESE', 'demo-2026-10', 'Рацион при ожирении', 1800, 85, 60, 180, $json$[
         {"meal":"Завтрак","items":"Белковый омлет с овощами, чай без сахара"},
         {"meal":"Второй завтрак","items":"Кефир или яблоко"},
         {"meal":"Обед","items":"Овощной суп, мясо или рыба, овощи"},
         {"meal":"Полдник","items":"Творог, огурец"},
         {"meal":"Ужин","items":"Рыба на пару, салат из овощей"}
       ]$json$);
