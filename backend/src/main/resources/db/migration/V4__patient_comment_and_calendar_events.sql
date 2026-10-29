alter table patient_card add column doctor_comment text;
alter table patient_card alter column diary_required set default true;

create table calendar_event
(
    id              bigserial primary key,
    doctor_id       bigint       not null references doctor (id) on delete cascade,
    patient_card_id bigint       references patient_card (id) on delete cascade,
    event_date      date         not null,
    event_time      time,
    title           varchar(200) not null,
    note            text,
    created_at      timestamptz  not null default now(),
    updated_at      timestamptz  not null default now()
);

create index ix_calendar_event_doctor_date on calendar_event (doctor_id, event_date);
create index ix_calendar_event_patient on calendar_event (patient_card_id);
