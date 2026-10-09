-- Scenarios and per-user progress (002-feature-scenario).
-- The scenario list is seeded here so new content ships without an app release (research R1);
-- placeholder objective titles are replaced by the content set authored elsewhere.

create table scenario (
    id text primary key,
    title text not null,
    position int not null unique,
    is_free boolean not null default false
);

create table scenario_objective (
    scenario_id text not null references scenario (id),
    objective_order int not null,
    title text not null,
    primary key (scenario_id, objective_order)
);

create table user_scenario (
    user_id uuid not null references users (id) on delete cascade,
    scenario_id text not null references scenario (id),
    status text not null,
    attempt int not null default 1,
    current_objective int not null default 1,
    opened_at timestamptz not null,
    completed_at timestamptz,
    primary key (user_id, scenario_id)
);

-- Insert-only achievement facts: progress can only move forward (FR-005).
create table user_objective (
    user_id uuid not null references users (id) on delete cascade,
    scenario_id text not null,
    attempt int not null,
    objective_order int not null,
    achieved_on date not null,
    primary key (user_id, scenario_id, attempt, objective_order)
);

-- Streak facts: device-local dates as reported, never deleted (research R4, R10).
create table practice_day (
    user_id uuid not null references users (id) on delete cascade,
    local_date date not null,
    primary key (user_id, local_date)
);

-- Also the per-user lock anchor serializing progress mutations (research R5).
create table user_stats (
    user_id uuid primary key references users (id) on delete cascade,
    practice_seconds bigint not null default 0
);

-- Idempotency ledger: one progress report is applied at most once (research R11).
create table progress_report (
    user_id uuid not null references users (id) on delete cascade,
    report_id uuid not null,
    received_at timestamptz not null,
    primary key (user_id, report_id)
);

insert into scenario (id, title, position, is_free) values
    ('cafe-visit', 'Поход в кафе', 1, true),
    ('small-talk', 'Знакомство и смолток', 2, false),
    ('taxi-transport', 'Такси и транспорт', 3, false),
    ('pharmacy-doctor', 'Аптека и врач', 4, false),
    ('hotel-checkin', 'Отель и заселение', 5, false),
    ('job-interview', 'Собеседование', 6, false),
    ('first-day-office', 'Первый день в офисе', 7, false),
    ('client-call', 'Разговор с клиентом', 8, false),
    ('price-negotiation', 'Переговоры о цене', 9, false),
    ('apartment-rent', 'Аренда квартиры', 10, false),
    ('shop-return', 'Магазин и возврат', 11, false),
    ('bank-account', 'Банк и счёт', 12, false),
    ('airport-flight', 'Аэропорт и рейс', 13, false),
    ('support-call', 'Звонок в поддержку', 14, false),
    ('gym-trainer', 'Спортзал и тренер', 15, false),
    ('salon-barber', 'Салон и парикмахер', 16, false),
    ('parent-meeting', 'Родительское собрание', 17, false),
    ('repair-master', 'Ремонт и мастер', 18, false),
    ('dating', 'Свидание', 19, false),
    ('party-toast', 'Тост и вечеринка', 20, false);

insert into scenario_objective (scenario_id, objective_order, title) values
    ('cafe-visit', 1, 'Заказ у стойки'),
    ('cafe-visit', 2, 'Вопрос о меню'),
    ('cafe-visit', 3, 'Просьба заменить ингредиент'),
    ('cafe-visit', 4, 'Уточнение счёта'),
    ('cafe-visit', 5, 'Вежливая жалоба'),
    ('cafe-visit', 6, 'Разговор с бариста'),
    ('small-talk', 1, 'Шаг 1'),
    ('small-talk', 2, 'Шаг 2'),
    ('small-talk', 3, 'Шаг 3'),
    ('small-talk', 4, 'Шаг 4'),
    ('small-talk', 5, 'Шаг 5'),
    ('taxi-transport', 1, 'Шаг 1'),
    ('taxi-transport', 2, 'Шаг 2'),
    ('taxi-transport', 3, 'Шаг 3'),
    ('taxi-transport', 4, 'Шаг 4'),
    ('taxi-transport', 5, 'Шаг 5'),
    ('taxi-transport', 6, 'Шаг 6'),
    ('pharmacy-doctor', 1, 'Шаг 1'),
    ('pharmacy-doctor', 2, 'Шаг 2'),
    ('pharmacy-doctor', 3, 'Шаг 3'),
    ('pharmacy-doctor', 4, 'Шаг 4'),
    ('pharmacy-doctor', 5, 'Шаг 5'),
    ('hotel-checkin', 1, 'Шаг 1'),
    ('hotel-checkin', 2, 'Шаг 2'),
    ('hotel-checkin', 3, 'Шаг 3'),
    ('hotel-checkin', 4, 'Шаг 4'),
    ('hotel-checkin', 5, 'Шаг 5'),
    ('job-interview', 1, 'Шаг 1'),
    ('job-interview', 2, 'Шаг 2'),
    ('job-interview', 3, 'Шаг 3'),
    ('job-interview', 4, 'Шаг 4'),
    ('job-interview', 5, 'Шаг 5'),
    ('job-interview', 6, 'Шаг 6'),
    ('first-day-office', 1, 'Шаг 1'),
    ('first-day-office', 2, 'Шаг 2'),
    ('first-day-office', 3, 'Шаг 3'),
    ('first-day-office', 4, 'Шаг 4'),
    ('first-day-office', 5, 'Шаг 5'),
    ('first-day-office', 6, 'Шаг 6'),
    ('client-call', 1, 'Шаг 1'),
    ('client-call', 2, 'Шаг 2'),
    ('client-call', 3, 'Шаг 3'),
    ('client-call', 4, 'Шаг 4'),
    ('client-call', 5, 'Шаг 5'),
    ('client-call', 6, 'Шаг 6'),
    ('price-negotiation', 1, 'Шаг 1'),
    ('price-negotiation', 2, 'Шаг 2'),
    ('price-negotiation', 3, 'Шаг 3'),
    ('price-negotiation', 4, 'Шаг 4'),
    ('price-negotiation', 5, 'Шаг 5'),
    ('price-negotiation', 6, 'Шаг 6'),
    ('apartment-rent', 1, 'Шаг 1'),
    ('apartment-rent', 2, 'Шаг 2'),
    ('apartment-rent', 3, 'Шаг 3'),
    ('apartment-rent', 4, 'Шаг 4'),
    ('apartment-rent', 5, 'Шаг 5'),
    ('shop-return', 1, 'Шаг 1'),
    ('shop-return', 2, 'Шаг 2'),
    ('shop-return', 3, 'Шаг 3'),
    ('shop-return', 4, 'Шаг 4'),
    ('shop-return', 5, 'Шаг 5'),
    ('bank-account', 1, 'Шаг 1'),
    ('bank-account', 2, 'Шаг 2'),
    ('bank-account', 3, 'Шаг 3'),
    ('bank-account', 4, 'Шаг 4'),
    ('bank-account', 5, 'Шаг 5'),
    ('airport-flight', 1, 'Шаг 1'),
    ('airport-flight', 2, 'Шаг 2'),
    ('airport-flight', 3, 'Шаг 3'),
    ('airport-flight', 4, 'Шаг 4'),
    ('airport-flight', 5, 'Шаг 5'),
    ('airport-flight', 6, 'Шаг 6'),
    ('support-call', 1, 'Шаг 1'),
    ('support-call', 2, 'Шаг 2'),
    ('support-call', 3, 'Шаг 3'),
    ('support-call', 4, 'Шаг 4'),
    ('support-call', 5, 'Шаг 5'),
    ('gym-trainer', 1, 'Шаг 1'),
    ('gym-trainer', 2, 'Шаг 2'),
    ('gym-trainer', 3, 'Шаг 3'),
    ('gym-trainer', 4, 'Шаг 4'),
    ('gym-trainer', 5, 'Шаг 5'),
    ('salon-barber', 1, 'Шаг 1'),
    ('salon-barber', 2, 'Шаг 2'),
    ('salon-barber', 3, 'Шаг 3'),
    ('salon-barber', 4, 'Шаг 4'),
    ('salon-barber', 5, 'Шаг 5'),
    ('parent-meeting', 1, 'Шаг 1'),
    ('parent-meeting', 2, 'Шаг 2'),
    ('parent-meeting', 3, 'Шаг 3'),
    ('parent-meeting', 4, 'Шаг 4'),
    ('parent-meeting', 5, 'Шаг 5'),
    ('repair-master', 1, 'Шаг 1'),
    ('repair-master', 2, 'Шаг 2'),
    ('repair-master', 3, 'Шаг 3'),
    ('repair-master', 4, 'Шаг 4'),
    ('repair-master', 5, 'Шаг 5'),
    ('repair-master', 6, 'Шаг 6'),
    ('dating', 1, 'Шаг 1'),
    ('dating', 2, 'Шаг 2'),
    ('dating', 3, 'Шаг 3'),
    ('dating', 4, 'Шаг 4'),
    ('dating', 5, 'Шаг 5'),
    ('dating', 6, 'Шаг 6'),
    ('party-toast', 1, 'Шаг 1'),
    ('party-toast', 2, 'Шаг 2'),
    ('party-toast', 3, 'Шаг 3'),
    ('party-toast', 4, 'Шаг 4'),
    ('party-toast', 5, 'Шаг 5');
